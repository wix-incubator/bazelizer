package com.wix.incubator.mvn;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maven project.
 */
@SuppressWarnings("Convert2MethodRef")
public class MavenProject {
    private final String id;
    private final String parentId;
    private final File parentPom;
    private final File pom;
    private final Model model;

    /**
     * New project from pom file
     * @param p a file
     * @return maven project
     */
    public static MavenProject create(Path p) {
        try {
            return new MavenProject(p.toFile());
        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * New projet from json description.
     * @param json json str
     * @return maven project
     */
    public static MavenProject create(String json) {
        final MavenProjectDTO dto = Cli.GSON.fromJson(json, MavenProjectDTO.class);
        try {
            return new MavenProject(dto.file.toFile());
        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class MavenProjectDTO {
        public Path file;
    }

    /**
     * Ctor.
     * @param pom a pom file
     * @throws XmlPullParserException if any
     * @throws IOException if any
     */
    private MavenProject(File pom) throws XmlPullParserException, IOException {
        try (FileInputStream is = new FileInputStream(pom)) {
            MavenXpp3Reader r = new MavenXpp3Reader();
            model = r.read(is);
        }
        this.id = pom.toPath().toAbsolutePath().toString();
        this.pom = pom;
        Optional<String> parentRelPath = Optional.ofNullable(model.getParent()).map(p -> p.getRelativePath());
        Optional<Path> parentAbsPath = parentRelPath.map(p -> pom.toPath().toAbsolutePath().getParent().resolve(p).normalize());
        this.parentId = parentAbsPath.map(p -> p.toString()).orElse(null);
        this.parentPom = parentAbsPath.map(p -> p.toFile()).orElse(null);
    }

    /**
     * Emit new pom file for execution.
     * @return pom file
     * @throws IOException if any
     */
    public File emit() throws IOException {
        final Path newPom = pom.getParentFile().toPath().resolve("pom.bazelizer.__gen__.xml");
        if (Files.notExists(newPom)) {
            Files.copy(pom.toPath(), newPom, StandardCopyOption.REPLACE_EXISTING);
        }
        return newPom.toFile();

    }

    /**
     * Jar file of a project.
     * @return a jar
     */
    public Path jar() {
        final Path target = pom.toPath().getParent().resolve("target");
        return target.resolve(String.format("%s-%s.jar", model.getArtifactId(), model.getVersion()));
    }

    public Path folder() {
        String groupId = model.getGroupId() == null ? model.getParent().getGroupId() : model.getGroupId();
        return Maven.mvnLayout(groupId, model.getArtifactId(), model.getVersion());
    }

    private MavenProject getParentProject() throws XmlPullParserException, IOException {
        return parentPom != null ? new MavenProject(parentPom) : null;
    }

    @Override
    public String toString() {
        return "" + model.getId() + "";
    }

    public static List<MavenProject> topologicSort(Iterable<MavenProject> projects) throws Exception {
        DAG dag = new DAG();
        Map<String, MavenProject> vertices = new HashMap<>();
        for (MavenProject project : projects) {
            addVertex(dag, vertices, project);
        }

        for (MavenProject project : projects) {
            if (project.parentId != null) {
                dag.addEdge(project.id, project.parentId);
            }
        }
        final List<String> ids = TopologicalSorter.sort(dag);
        return ids.stream().map(id -> vertices.get(id)).collect(Collectors.toList());
    }


    private static void addVertex(DAG dag, Map<String, MavenProject> vertices,
                                  MavenProject project) throws XmlPullParserException, IOException {
        if (project == null)
            return;

        dag.addVertex(project.id);
        vertices.putIfAbsent(project.id, project);
        addVertex(dag, vertices, project.getParentProject());
    }


}
