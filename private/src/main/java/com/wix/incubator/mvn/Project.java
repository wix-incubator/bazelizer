package com.wix.incubator.mvn;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maven project.
 */
public class Project {

    @AllArgsConstructor
    public static class Output {
        final String src;
        final Path dest;
    }

    @AllArgsConstructor
    public static class PomFile {
        public final File file;
        public final Model model;

        public Path target() {
            return file.toPath().getParent().resolve("target").toAbsolutePath();
        }
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
        final MavenProjectDTO dto = Cli.GSON.fromJson(json, MavenProjectDTO.class);
        return new Project(dto.file.toFile(), dto.flags);
    }

    private static class MavenProjectDTO {
        public Path file;
        public List<String> flags;
    }

    public static List<Project> sortProjects(List<Project> projects) throws CycleDetectedException {
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
        return ids.stream().map(vertices::get).filter(Objects::nonNull).collect(Collectors.toList());
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

    private final Model model;
    private final String id;
    private final String parentId;
    private final File pomSrcFile;
    private final File pomParentFile;

    @Getter
    private final Args args;

    private Project(File file) {
        this(file, Collections.emptyList());
    }

    private Project(File file, List<String> flags) {
        try (FileInputStream is = new FileInputStream(file)) {
            MavenXpp3Reader r = new MavenXpp3Reader();
            this.model = r.read(is);
        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }

        Optional<Path> parentAbsPath = Optional.ofNullable(model.getParent())
                .map(Parent::getRelativePath)
                .map(p -> file.toPath().toAbsolutePath().getParent().resolve(p).normalize());
        this.parentId = parentAbsPath.map(Path::toString).orElse(null);
        this.pomParentFile = parentAbsPath.map(Path::toFile).orElse(null);
        this.args = createArgs(this.model, flags);
        this.pomSrcFile = file;
        this.id = generateId(file, args);
    }

    private static String generateId(File file, Args args) {
        return file.toPath().toAbsolutePath() + ":" + args.toHash();
    }

    private static Project.Args createArgs(Model model, List<String> flags) {
        if (!flags.isEmpty()) {
            final Cmd.ExecutionOpts options = new Cmd.ExecutionOpts();
            final CommandLine.ParseResult result = new CommandLine(options)
                    .parseArgs(flags.toArray(new String[0]));
            if (!result.errors().isEmpty()) {
                Console.error(model, "project flags are invalid:");
                result.errors().forEach(e -> Console.error(model, " -" + e.getMessage()));
                throw new IllegalArgumentException("invalid flags for project {" + model + "}");
            }
            return Project.Args.builder()
                    .profiles(options.mavenActiveProfiles)
                    .modelVisitor(options.visitor())
                    .cmd(options.mavenArgs)
                    .build();
        } else {
            return Project.Args.builder().build();
        }
    }

    /**
     * Save pom file.
     *
     * @param args deps
     * @return file
     * @throws IOException if any
     */
    public PomFile emitPom(Project.Args args) throws IOException {
        final Path newPom = pomSrcFile.getParentFile().toPath().resolve("pom.__bazelizer__.xml");
        final Model newModel = model.clone();
        args.modelVisitor.apply(
                newModel
        );

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
        return new PomFile(newPom.toFile(), newModel);
    }

    @Override
    public String toString() {
        String suf = args.toHash();
        if (!suf.isEmpty()) suf = "/" + suf;
        return model.toString() + suf;
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
                    .cmd(Stream.concat(cmd.stream(), other.cmd.stream()).distinct().collect(Collectors.toList()))
                    .modelVisitor(modelVisitor.andThen(other.modelVisitor))
                    .deps(Stream.concat(deps.stream(), other.deps.stream()).collect(Collectors.toList()))
                    .profiles(Stream.concat(profiles.stream(), other.profiles.stream()).collect(Collectors.toList()))
                    .build();
        }

        @Override
        public String toString() {
            final String cmdStr = String.join(" ", cmd);
            final String profiles = this.profiles.stream()
                    .map(s -> "-P " + s).collect(Collectors.joining(" "));
            return "[" + cmdStr + (profiles.isEmpty() ? "" : " " + profiles) + "]";
        }

        @SuppressWarnings("UnstableApiUsage")
        public String toHash() {
            final Iterable<String> str = Iterables.concat(cmd, profiles);
            return Iterables.isEmpty(str) ? ""
                    : Hashing.murmur3_32()
                    .hashString(String.join(" ", str), StandardCharsets.UTF_8).toString();

        }
    }


    public interface ModelVisitor {
        ModelVisitor NOP = d -> {};

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

    @AllArgsConstructor
    public static class ChangeArtifactId implements ModelVisitor {
        private final String id;

        @Override
        public void apply(Model model) {
            model.setArtifactId(id);
        }
    }

}
