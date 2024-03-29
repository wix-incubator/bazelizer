package com.wix.incubator.mvn;

import com.github.mustachejava.Mustache;
import com.google.common.io.CharSource;
import com.google.devtools.build.runfiles.Runfiles;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static com.google.common.collect.Lists.newArrayList;
import static com.wix.incubator.mvn.Cli.BZL_MVN_TOOL_SYS_PROP;
import static com.wix.incubator.mvn.IOSupport.REPOSITORY_FILES_FILTER;

@SuppressWarnings("FieldCanBeLocal")
public class Maven {

    /**
     * Get layout of folder according to maven coordinates.
     *
     * @param groupId    group id
     * @param artifactId artifact id
     * @param version    version
     * @return a path
     */
    public static Path artifactRepositoryLayout(String groupId, String artifactId, String version) {
        String[] gidParts = groupId.split("\\.");
        Path thisGroupIdRepo = Paths.get("");
        for (String gidPart : gidParts) {
            thisGroupIdRepo = thisGroupIdRepo.resolve(gidPart);
        }
        return thisGroupIdRepo.resolve(artifactId).resolve(version);
    }

    /**
     * Prepare maven environment from archived repository.
     *
     * @param repositoryArchive archived tar
     * @return a maven
     * @throws IOException if any
     */
    public static Maven prepareEnvFromArchive(Path repositoryArchive) throws IOException {
        final Maven env = prepareEnv(Collections.emptyList());
        IOSupport.untar(repositoryArchive, env.repository);
        return env;
    }

    /**
     * Prepare default maven env.
     *
     * @return a maven
     * @throws IOException if any
     */
    @SuppressWarnings({"UnstableApiUsage"})
    public static Maven prepareEnv(List<MvnRepository> repositories) throws IOException {
        Runfiles runfiles = Cli.RUNFILES;
        final File tool = Optional.ofNullable(System.getProperty(BZL_MVN_TOOL_SYS_PROP))
                .map((runfilesPath) -> new File(runfiles.rlocation(runfilesPath)))
                .orElseThrow(() -> new IllegalStateException("no sys prop: " + BZL_MVN_TOOL_SYS_PROP));

        Path m2HomeDir = IOSupport.newTempDirectory("M2_HOME@").toPath();
        Console.info(" M2_HOME=" + m2HomeDir.toAbsolutePath());
        Path repository = m2HomeDir.resolve("repository").toAbsolutePath();
        Files.createDirectories(repository);
        Path settingsXmlFile = m2HomeDir.resolve("settings.xml").toAbsolutePath();

        final CharSource tmpl = asCharSource(
                getResource("settings.xml.mustache"), StandardCharsets.UTF_8);
        Map<String, Object> scope = new HashMap<>();
        scope.put("localRepository", repository.toAbsolutePath().toString());
        scope.put("profiles", repositories);

        try (Reader r = tmpl.openStream()) {
            try (Writer out = Files.newBufferedWriter(settingsXmlFile,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING)) {
                final Mustache mustache = Cli.MUSTACHE.compile(r, "s");
                mustache.execute(out, scope);
            }
        }

        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(tool);
        return new Maven(m2HomeDir, repository, settingsXmlFile, invoker);
    }

    @SuppressWarnings("unused")
    private final Path m2HomeDir;
    private final Path settingsXmlFile;
    public final Path repository;
    private final Invoker maven;

    /**
     * Ctor.
     */
    private Maven(Path m2HomeDir, Path repository, Path settingsXmlFile, Invoker maven) {
        this.m2HomeDir = m2HomeDir;
        this.repository = repository;
        this.settingsXmlFile = settingsXmlFile;
        this.maven = maven;
    }

    /**
     * Execute maven build in offline mode.
     *
     * @param project a project
     * @param args    args
     * @throws IOException              if any
     * @throws MavenInvocationException if any
     */
    public void executeOffline(Project project, Project.Args args, Iterable<Out> outs) throws IOException, MavenInvocationException {
        Console.printSeparator();
        logMavenVersion();
        Console.printSeparator();

        executeIntern(project, args, true, outs);
    }

    /**
     * Execute regular maven build in reactor order.
     *
     * @param projects the projects
     * @param args     args
     * @throws IOException              if any
     * @throws MavenInvocationException if any
     */
    public void executeInOrder(List<Project> projects, Project.Args args) throws IOException, MavenInvocationException, CycleDetectedException {
        Console.info("Projects reactor order:");
        final List<Project> reactorOrder = Project.sortProjects(projects);
        for (Project project : reactorOrder) {
            Console.info("\t{"+project+"}");
        }

        Console.printSeparator();
        logMavenVersion();
        Console.printSeparator();

        for (Project project : reactorOrder) {
            executeIntern(project, args, false, Collections.emptyList());
        }
    }

    private void executeIntern(Project project, Project.Args inputArgs, boolean offline, Iterable<Out> outputs) throws IOException, MavenInvocationException {
        final Project.Args args = inputArgs.merge(project.getArgs());
        for (Dep dep : args.deps) {
            dep.installTo(repository);
        }
        final Project.PomFile pom = project.emitPom(args);
        final File pomFile = pom.file;

        maven.setWorkingDirectory(pomFile.getParentFile());
        DefaultInvocationRequest request = newInvocationRequest();
        request.setUserSettingsFile(settingsXmlFile.toFile());
        request.setLocalRepositoryDirectory(repository.toFile());
        request.setBatchMode(true);
        request.setShowVersion(false);

        request.setPomFile(pomFile);
        request.setGoals(args.cmd);
        request.setProfiles(args.profiles);
        request.setOffline(offline);
        Console.info(project, " >>>> Executing commands " + args);
        request.setOutputHandler(new Console.PrintOutputHandler());

        long x0 = System.currentTimeMillis();
        final InvocationResult result = maven.execute(request);
        long x1 = System.currentTimeMillis();

        if (result.getExitCode() != 0) {
            Console.error(project, " >>>> Build failed. Dump pom file");
            Console.dumpXmlFile(pomFile);
            throw new MvnExecException("non zero exit code: " + result.getExitCode());
        }

        Console.info(project, " >>>> Done. Elapsed time: " + duration(x0, x1));

        for (Out out : outputs) {
            out.save(Maven.this, pom);
        }
    }

    /**
     * Tar repository into output
     * @param out path
     * @return size of archive
     * @throws IOException if any error
     */
    public long tarRepositoryRecursive(Path out) throws IOException {
        final Path dir = this.repository;
        final Collection<Path> files = FileUtils.listFiles(
                dir.toFile(), REPOSITORY_FILES_FILTER, FileFilterUtils.directoryFileFilter() // recursive
        ).stream().map(File::toPath).collect(Collectors.toList());
        try (OutputStream os = Files.newOutputStream(out,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING)) {
            return IOSupport.tar(files, os, dir::relativize);
        }
    }

    private void logMavenVersion() {
        try {
            DefaultInvocationRequest request = newInvocationRequest("INFO");
            request.setShowErrors(false);
            request.setGoals(newArrayList("-v"));
            maven.execute(request);
        } catch (MavenInvocationException ignored) {
        }
    }

    private DefaultInvocationRequest newInvocationRequest() {
        return newInvocationRequest("INFO");
    }

    private DefaultInvocationRequest newInvocationRequest(String logLvl) {
        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setShowVersion(true);
        request.setJavaHome(new File(System.getProperty("java.home")));
        Properties properties = request.getProperties();
        if (properties == null) {
            properties = new Properties();
        }
        properties.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLvl);
        request.setProperties(properties);
        return request;
    }

    private String duration(long from, long to) {
        final Duration dur = Duration.ofMillis(to - from);
        return dur.getSeconds() + "." + dur.minusSeconds(dur.getSeconds()).toMillis() + "s";
    }

    public static class MvnExecException extends IOException {
        public MvnExecException(String message) {
            super(message);
        }
    }
}
