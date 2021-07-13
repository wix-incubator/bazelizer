package com.wix.incubator.mvn;

import com.github.mustachejava.Mustache;
import com.google.common.io.CharSource;
import com.google.devtools.build.runfiles.Runfiles;
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
import picocli.CommandLine;

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
import java.util.stream.Stream;

import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;

@SuppressWarnings("FieldCanBeLocal")
public class Maven {


    @SuppressWarnings("unused")
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
        return new Project(p.toFile(), Collections.emptyList());
    }

    /**
     * New projet from json description.
     *
     * @param json json str
     * @return maven project
     */
    public static Project createProject(String json) {
        final MavenProjectDTO dto = Cli.GSON.fromJson(json, Maven.MavenProjectDTO.class);
        return new Project(dto.file.toFile(), dto.flags);
    }

    private static class MavenProjectDTO {
        public Path file;
        public List<String> flags;
    }

    /**
     * Maven project.
     */
    public static class Project {
        private final Model model;
        private final String id;
        private final String parentId;
        private final File pomSrcFile;
        private final File pomParentFile;
        private final Args predefinedArgs;

        private Project(File file) {
            this(file, Collections.emptyList());
        }

        private Project(File file, List<String> flags) {
            try (FileInputStream is = new FileInputStream(file)) {
                MavenXpp3Reader r = new MavenXpp3Reader();
                model = r.read(is);
            } catch (XmlPullParserException | IOException e) {
                throw new IllegalStateException(e);
            }

            this.id = file.toPath().toAbsolutePath().toString();
            this.pomSrcFile = file;

            Optional<Path> parentAbsPath = Optional.ofNullable(model.getParent())
                    .map(Parent::getRelativePath)
                    .map(p -> file.toPath().toAbsolutePath().getParent().resolve(p).normalize());
            this.parentId = parentAbsPath.map(Path::toString).orElse(null);
            this.pomParentFile = parentAbsPath.map(Path::toFile).orElse(null);
            if (!flags.isEmpty()) {
                final Cli.ExecutionOptions options = new Cli.ExecutionOptions();
                new CommandLine(options).parseArgs(flags.toArray(new String[0]));
                predefinedArgs = Args.builder()
                        .profiles(options.mavenActiveProfiles)
                        .modelVisitor(options.visitor())
                        .build();
            } else {
                predefinedArgs = Args.builder().build();
            }
        }

        /**
         * Save pom file.
         *
         * @param args deps
         * @return file
         * @throws IOException if any
         */
        public File emitPom(Args args) throws IOException {
            final Path newPom = pomSrcFile.getParentFile().toPath().resolve("pom.__bazelizer__.xml");
            final Model newModel = model.clone();
            args.modelVisitor.apply(newModel);

            for (Dep dep : args.deps) {
                final Dependency d = new Dependency();
                d.setArtifactId(dep.artifactId);
                d.setGroupId(dep.groupId);
                d.setVersion(dep.version);
                d.setScope(dep.scope());
                newModel.getDependencies().add(d);
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
            final Path target = pomSrcFile.toPath().getParent().resolve("target");
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
    public void executeInOrder(List<Project> projects, Args args) throws IOException, MavenInvocationException, CycleDetectedException {
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


    private void executeIntern(Project project, Args inputArgs, boolean offline) throws IOException, MavenInvocationException {
        final Args args = inputArgs.merge(project.predefinedArgs);
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
        Log.info(project, "");
        Log.info(project, " >>>> executing commands " + args);
        request.setOutputHandler(new Log.PrintOutputHandler());

        long x0 = System.currentTimeMillis();
        final InvocationResult result = maven.execute(request);
        long x1 = System.currentTimeMillis();

        if (result.getExitCode() != 0) {
            Log.error(project, " >>>> Build failed. Dump pom file");
            Log.dumpXmlFile(pomFile);
            throw new MvnExecException("non zero exit code: " + result.getExitCode());
        }

        Log.info(project, " >>>> Done. Elapsed time: " + duration(x0, x1));
        Log.info(project, "");
    }

    private String duration(long from, long to) {
        final Duration dur = Duration.ofMillis(to - from);
        return dur.getSeconds() + "." + dur.minusSeconds(dur.getSeconds()).toMillis() + "s";
    }

    private static void addVertex(DAG dag, Map<String, Project> vertices, Project project) {
        if (project == null) return;
        dag.addVertex(project.id);
        vertices.putIfAbsent(project.id, project);
        addVertex(dag, vertices, getParent(project).orElse(null));
    }

    private static Optional<Project> getParent(Project p) {
        return Optional.ofNullable(p.pomParentFile).map(Project::new);
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
        public final ModelVisitor modelVisitor = d -> {};
        @Builder.Default
        public final List<String> profiles = Collections.emptyList();

        public Args merge(Args other) {
            return Args.builder()
                    .cmd(Stream.concat(cmd.stream(), other.cmd.stream()).collect(Collectors.toList()))
                    .modelVisitor(modelVisitor.andThen(other.modelVisitor))
                    .deps(Stream.concat(deps.stream(), other.deps.stream()).collect(Collectors.toList()))
                    .profiles(Stream.concat(profiles.stream(), other.profiles.stream()).collect(Collectors.toList()))
                    .build();
        }

        @Override
        public String toString() {
            final String cmdStr = String.join(" ", cmd);
            final String profiles = this.profiles.stream().map(s -> "-P " + s)
                    .collect(Collectors.joining(" "));
            return "[" + cmdStr + (profiles.isEmpty() ? "" : " " + profiles) + "]";
        }
    }


    public interface ModelVisitor {
        void apply(Model model);

        default ModelVisitor andThen(ModelVisitor after) {
            Objects.requireNonNull(after);
            return (t) -> { apply(t); after.apply(t); };
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class DropAllDepsModelVisitor implements ModelVisitor {
        private Predicate<Dependency> excludeFilter = null;

        public DropAllDepsModelVisitor addIgnores(Collection<String> c) {
            c.forEach(this::addIgnore);
            return this;
        }

        public DropAllDepsModelVisitor addIgnore(String coords) {
            excludeFilter = Optional.ofNullable(excludeFilter)
                    .map(other -> other.or(matchCoordsExpr(coords)))
                    .orElseGet(() -> matchCoordsExpr(coords));
            return this;
        }

        @Override
        public void apply(Model model) {
            model.getDependencies().removeIf(d -> excludeFilter == null || !excludeFilter.test(d));
        }
    }

    /**
     * Rule accept only by maven coordinates pattern
     *
     * @param coords pattern
     */
    private static Predicate<Dependency> matchCoordsExpr(String coords) {
        final int split = coords.indexOf(":");
        if (split == -1) throw new IllegalArgumentException(
                "illegal expression be in format of'<artifactId|*>:<groupId|*>' ");
        String groupIdPtn = coords.substring(0, split);
        String artifactIdPtn = coords.substring(split + 1);
        final Predicate<Dependency> groupIdFilter = matchExpression(groupIdPtn, Dependency::getGroupId);
        final Predicate<Dependency> artifactIdFilter = matchExpression(artifactIdPtn, Dependency::getArtifactId);
        return groupIdFilter.and(artifactIdFilter);
    }

    private static Predicate<Dependency> matchExpression(String coords, Function<Dependency, String> fn) {
        if (coords.equals("*"))
            return d -> true;
        if (coords.startsWith("*")) {
            final int split = coords.indexOf("*");
            String suf = coords.substring(split + 1);
            return d -> fn.apply(d).endsWith(suf);
        }
        if (coords.endsWith("*")) {
            final int split = coords.indexOf("*");
            String pref = coords.substring(0, split);
            return d -> fn.apply(d).startsWith(pref);
        }
        return d -> fn.apply(d).equals(coords);
    }
}
