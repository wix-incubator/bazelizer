package com.wix.incubator.mvn;

import com.github.mustachejava.Mustache;
import com.google.common.io.CharSource;
import com.google.devtools.build.runfiles.Runfiles;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;

@SuppressWarnings("FieldCanBeLocal")
public class Maven {


    public static class MvnRepository {
        public String id;
        public String url;

        public static List<MvnRepository> fromFile(Path file) throws IOException {
            return IOSupport.readLines(file).stream()
                    .map(json -> Cli.GSON.fromJson(json, MvnRepository.class))
                    .collect(Collectors.toList());
        }
    }

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
     * New project from pom file
     *
     * @param p a file
     * @return maven project
     */
    public static Project createProject(Path p) {
        return new Project(p.toFile());
    }

    /**
     * New projet from json description.
     *
     * @param json json str
     * @return maven project
     */
    public static Project createProject(String json) {
        final MavenProjectDTO dto = Cli.GSON.fromJson(json, Maven.MavenProjectDTO.class);
        return new Project(dto.file.toFile());
    }

    private static class MavenProjectDTO {
        public Path file;
    }

    public static class Project {
        private final Model model;
        private final String id;
        private final String parentId;
        private final File srcFile;
        private final File parentFile;

        private Project(File file) {
            try (FileInputStream is = new FileInputStream(file)) {
                MavenXpp3Reader r = new MavenXpp3Reader();
                model = r.read(is);
            } catch (XmlPullParserException | IOException e) {
                throw new IllegalStateException(e);
            }

            this.id = file.toPath().toAbsolutePath().toString();
            this.srcFile = file;
            Optional<Path> parentAbsPath = Optional.ofNullable(model.getParent())
                    .map(Parent::getRelativePath)
                    .map(p -> file.toPath().toAbsolutePath().getParent().resolve(p).normalize());

            this.parentId = parentAbsPath.map(Path::toString).orElse(null);
            this.parentFile = parentAbsPath.map(Path::toFile).orElse(null);
        }

        private Optional<Project> getParent() {
            return Optional.ofNullable(parentFile).map(Project::new);
        }

        /**
         * Save pom file.
         *
         * @param args deps
         * @return file
         * @throws IOException if any
         */
        public File emitPom(Args args) throws IOException {
            final Path newPom = srcFile.getParentFile().toPath().resolve("pom.__bazelizer__.xml");
            final Model newModel = model.clone();
            final List<Dependency> dependencies = newModel.getDependencies();
            dependencies.removeIf(d -> !args.depsFilter.test(d));

            for (Dep dep : args.deps) {
                final Dependency d = new Dependency();
                d.setArtifactId(dep.artifactId);
                d.setGroupId(dep.groupId);
                d.setVersion(dep.version);
                d.setScope(dep.scope());
                dependencies.add(d);
            }
            if (!newPom.toFile().exists()) {
                final MavenXpp3Writer writer = new MavenXpp3Writer();
                try (OutputStream out = Files.newOutputStream(newPom,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(out, newModel);
                }
            }
            return newPom.toFile();
        }


        public void save(Maven maven, Path jarOutput, Path archive) throws IOException {
            final Path target = srcFile.toPath().getParent().resolve("target");
            final Path jar = target.resolve(String.format("%s-%s.jar",
                    model.getArtifactId(), model.getVersion()));
            Files.copy(jar, jarOutput);

            String groupId = model.getGroupId() == null ? model.getParent().getGroupId() : model.getGroupId();
            Path installedFolder = Maven.artifactRepositoryLayout(groupId, model.getArtifactId(), model.getVersion());
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

        @Override
        public String toString() {
            return model.toString();
        }
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

        Path m2HomeDir = Files.createTempDirectory("M2_HOME@_" + "128" + "_@");
        Log.info(" M2_HOME=" + m2HomeDir);
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
    public void executeOffline(Project project, Args args) throws IOException, MavenInvocationException {
        executeIntern(project, args, true);
    }

    /**
     * Execute regular maven build in reactor order.
     *
     * @param projects the projects
     * @param args     args
     * @throws IOException              if any
     * @throws MavenInvocationException if any
     */
    public void execute(List<Project> projects, Args args) throws IOException, MavenInvocationException, CycleDetectedException {
        DAG dag = new DAG();
        Map<String, Project> vertices = new HashMap<>();
        for (Project project : projects) {
            addVertex(dag, vertices, project);
        }
        for (Project project : projects) {
            if (project.parentId != null) {
                dag.addEdge(project.id, project.parentId);
            }
        }
        final List<String> ids = TopologicalSorter.sort(dag);
        final List<Project> reactorOrder = ids.stream().map(vertices::get).collect(Collectors.toList());

        for (Project project : reactorOrder) {
            executeIntern(project, args, false);
        }
    }


    private void executeIntern(Project project, Args args, boolean offline) throws IOException, MavenInvocationException {
        for (Dep dep : args.deps) {
            dep.installTo(repository);
        }
        final File pomFile = project.emitPom(args);

        maven.setWorkingDirectory(pomFile.getParentFile());
        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setUserSettingsFile(settingsXmlFile.toFile());
        request.setLocalRepositoryDirectory(repository.toFile());
        request.setJavaHome(new File(System.getProperty("java.home")));
        request.setBatchMode(true);
        request.setShowVersion(true);

        request.setPomFile(pomFile);
        request.setGoals(args.cmd);
        request.setProfiles(args.profiles);
        request.setOffline(offline);
        Properties properties = request.getProperties();
        if (properties == null) {
            properties = new Properties();
        }
        properties.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
        request.setProperties(properties);
        Log.info(project, " >>>");
        Log.info(project, " >>> executing commands " + args);
        request.setOutputHandler(new Log.PrintOutputHandler());

        long x0 = System.currentTimeMillis();
        final InvocationResult result = maven.execute(request);
        long x1 = System.currentTimeMillis();

        if (result.getExitCode() != 0) {
            Log.error(project, " >>> Build failed");
            Log.dumpXmlFile(pomFile);
            throw new MvnExecException("non zero exit code: " + result.getExitCode());
        }

        Log.info(project, " >>> Done. Elapsed time: " + duration(x0, x1));
    }

    private String duration(long from, long to) {
        final Duration dur = Duration.ofMillis(to - from);
        return dur.getSeconds() + "." + dur.minusSeconds(dur.getSeconds()).toMillis() + "s";
    }

    private static void addVertex(DAG dag, Map<String, Project> vertices, Project project) {
        if (project == null) return;
        dag.addVertex(project.id);
        vertices.putIfAbsent(project.id, project);
        addVertex(dag, vertices, project.getParent().orElse(null));
    }

    public static class MvnExecException extends IOException {
        public MvnExecException(String message) {
            super(message);
        }
    }

    @AllArgsConstructor
    @Builder
    public static class Args {
        @Builder.Default
        public final List<String> cmd = Collections.emptyList();
        @Builder.Default
        public final List<Dep> deps = Collections.emptyList();
        @Builder.Default
        public final DepsFilter depsFilter = DepsFilter.trueFilter();
        @Builder.Default
        public final List<String> profiles = Collections.emptyList();

        @Override
        public String toString() {
            final String cmdStr = String.join(" ", cmd);
            final String profiles = this.profiles.stream().map(s -> "-P " + s)
                    .collect(Collectors.joining(" "));
            return "[" + cmdStr + (profiles.isEmpty() ? "" : " " + profiles) + "]";
        }
    }


    public interface DepsFilter extends Predicate<Dependency> {

        @Override
        default DepsFilter or(Predicate<? super Dependency> other) {
            return (t) -> test(t) || other.test(t);
        }

        /**
         * Rule that false by default.
         */
        static DepsFilter falseFilter() {
            return d -> false;
        }

        static DepsFilter trueFilter() {
            return d -> true;
        }

        /**
         * Rule accept only by maven coordinates pattern
         *
         * @param coords pattern
         */
        static DepsFilter coords(String coords) {
            final int split = coords.indexOf(":");
            if (split == -1) throw new IllegalArgumentException(
                    "illegal expression be in format of'<artifactId|*>:<groupId|*>' ");
            String groupIdPtn = coords.substring(0, split);
            String artifactIdPtn = coords.substring(split + 1);
            final DepsFilter groupIdFilter = coordsFilter(groupIdPtn, Dependency::getGroupId);
            final DepsFilter artifactIdFilter = coordsFilter(artifactIdPtn, Dependency::getArtifactId);
            Predicate<Dependency> rule = groupIdFilter.or(artifactIdFilter);
            return rule::test;
        }
    }

    private static DepsFilter coordsFilter(String coords, Function<Dependency, String> fn) {
        if (coords.equals("*")) return DepsFilter.trueFilter();
        final int split = coords.indexOf("*");
        if (coords.startsWith("*")) {
            String suf = coords.substring(split + 1);
            return d -> fn.apply(d).endsWith(suf);
        }
        if (coords.endsWith("*")) {
            String pref = coords.substring(0, split);
            return d -> fn.apply(d).startsWith(pref);
        }
        return d -> fn.apply(d).equals(coords);
    }
}
