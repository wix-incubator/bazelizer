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

@SuppressWarnings("Convert2MethodRef")
public class MavenProject {
    private final String id;
    private final String parentId;
    private final File parentPom;
    private final File pom;
    private final Model model;

    public static MavenProject create(Path p) {
        try {
            return new MavenProject(p.toFile());
        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static MavenProject create(String json) {
        final DTO dto = Cli.GSON.fromJson(json, DTO.class);
        try {
            return new MavenProject(dto.file.toFile());
        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

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

    private MavenProject getParentProject() throws XmlPullParserException, IOException {
        return parentPom != null ? new MavenProject(parentPom) : null;
    }

    public File emit() throws IOException {
        final Path newPom = pom.getParentFile().toPath().resolve("pom.bazelizer.__gen__.xml");
        if (Files.notExists(newPom)) {
            Files.copy(pom.toPath(), newPom, StandardCopyOption.REPLACE_EXISTING);
        }
        return newPom.toFile();

    }

    public Path jar() {
        final Path target = pom.toPath().getParent().resolve("target");
        return target.resolve(String.format("%s-%s.jar", model.getArtifactId(), model.getVersion()));
    }

    public static List<MavenProject> sort(Iterable<MavenProject> projects) throws Exception {
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

    private static class DTO {
        public Path file;
    }

    @Override
    public String toString() {
        return "" + model.getId() + "";
    }
}
