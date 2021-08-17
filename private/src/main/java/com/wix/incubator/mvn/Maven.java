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
     * Prepare maven environemtn from archived repository.
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
        Runfiles runfiles = Runfiles.create();
        final File tool = Optional.ofNullable(System.getProperty(BZL_MVN_TOOL_SYS_PROP))
                .map((runfilesPath) -> new File(runfiles.rlocation(runfilesPath)))
                .orElseThrow(() -> new IllegalStateException("no sys prop: " + BZL_MVN_TOOL_SYS_PROP));

        Path m2HomeDir = IOSupport.newTempDirectory("M2_HOME@").toPath();
        Console.info(" M2_HOME=" + m2HomeDir);
        Path repository = m2HomeDir.resolve("repository").toAbsolutePath();
        Files.createDirectories(repository);
        Path settingsXmlFile = m2HomeDir.resolve("settings.xml").toAbsolutePath();

        final CharSource tmpl = asCharSource(
                getResource("settings.xml.mustache"), StandardCharsets.UTF_8);
        Map<String, Object> scope = new HashMap<>();
        scope.put("localRepository", repository.toAbsolutePath().toString());
        scope.put("profiles", repositories);

        try (Reader r = tmpl.openStream()) {
            try (Writer out = Files.newBufferedWriter(settingsXmlFile, StandardOpenOption.CREATE)) {
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

    private static final String PREF = "tools.jvm.mvn.";
    private static final String BZL_MVN_TOOL_SYS_PROP = PREF + "MavenBin";

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
    public MvnSavable executeOffline(Project project, Project.Args args) throws IOException, MavenInvocationException {
        Console.printSeparator();
        logMavenVersion();
        Console.printSeparator();

        return executeIntern(project, args, true);
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
            executeIntern(project, args, false);
        }
    }

    private MvnSavable executeIntern(Project project, Project.Args inputArgs, boolean offline) throws IOException, MavenInvocationException {
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

        return new MvnSavable(pom);
    }

    private void logMavenVersion() {
        try {
            maven.execute(newInvocationRequest("OFF"));
        } catch (MavenInvocationException ignored) {
        }
    }

    private DefaultInvocationRequest newInvocationRequest() {
        return newInvocationRequest("WARN");
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


    @AllArgsConstructor
    public class MvnSavable {
        private final Project.PomFile pom;

        public void save(Iterable<Out> outs) {
            for (Out out : outs) {
                out.save(Maven.this, pom);
            }
        }
    }

    public abstract static class Out {
        public abstract void save(Maven maven, Project.PomFile pom);
    }

    @AllArgsConstructor
    public static class OutJar extends Out  {
        private final Path jarOutput;

        @SneakyThrows
        public void save(Maven maven, Project.PomFile pom) {
            final Path target = pom.target();
            final Path jar = target.resolve(String.format("%s-%s.jar", pom.model.getArtifactId(), pom.model.getVersion()));
            Files.copy(jar, jarOutput);
        }
    }


    @AllArgsConstructor
    public static class OutInstalled extends Out  {
        private final Path archive;

        @SneakyThrows
        public void save(Maven maven, Project.PomFile pom) {
            final String groupId = pom.model.getGroupId() == null ? pom.model.getParent().getGroupId() : pom.model.getGroupId();
            final Path installedFolder = Maven.artifactRepositoryLayout(groupId, pom.model.getArtifactId(), pom.model.getVersion());
            Collection<Path> files = FileUtils.listFiles(
                    maven.repository.resolve(installedFolder).toFile(),
                    FileFilterUtils.and(
                            IOSupport.REPOSITORY_FILES_FILTER,
                            FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("pom"))
                    ),
                    FileFilterUtils.trueFileFilter()
            ).stream().map(File::toPath).collect(Collectors.toList());

            try (OutputStream output = Files.newOutputStream(archive)) {
                IOSupport.tar(files, output, aFile -> {
                    final Path filePath = aFile.toAbsolutePath();
                    return filePath.subpath(maven.repository.getNameCount(), filePath.getNameCount());
                });
            }

        }
    }

    @AllArgsConstructor
    public static class OutFile extends Out  {
        private final String src;
        private final Path dest;

        @SneakyThrows
        @Override
        public void save(Maven maven, Project.PomFile pom) {
            Path target = pom.target();
            final Path srcFile = target.resolve(src);
            Files.copy(srcFile, dest);
        }
    }
}
