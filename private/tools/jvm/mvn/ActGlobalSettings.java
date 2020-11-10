package tools.jvm.mvn;

import com.google.common.collect.ImmutableMap;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.cactoos.Input;
import org.cactoos.Output;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.io.InputStreamOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.ResourceOf;
import org.xembly.Directives;
import org.xembly.Xembler;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@Slf4j
@AllArgsConstructor
public class ActGlobalSettings implements Act {


    /**
     * Current build manifest.
     */
    private final Input globalSettingsXml;

    /**
     * Output run manifest {@link RunManifest}
     */
    private final Output runManifestOutput;

    /**
     * optional tar archive of a repo.
     */
    private Path repositorySnapshot;

    @SneakyThrows
    @Override
    public Project accept(Project project) {
        final XML currentSettingsXml = new XMLDocument(
                new InputStreamOf(globalSettingsXml)
        );
        final File bazelLocalRepository = new File(
                currentSettingsXml.xpath("/settings/localRepository/text()").get(0)
        );


        final RunManifest.Builder builder = new RunManifest.Builder();
        Project projectNew = project;
        final ImmutableMap.Builder<Object, Object> props = ImmutableMap.builder();
        props.put("localRepository", "{{ localRepository }}");

        // use filesystem global repository for each build
        // populate it by repository maker call
        if (repositorySnapshot == null) {
            projectNew = project.toBuilder()
                    .m2Directory(bazelLocalRepository.toPath().getParent())
                    .build();

            props.put("mirroring", true);
            props.put("mirrorUrl", bazelLocalRepository.toURI().toString());
            props.put("mirrorId", SysProps.workspace().orElse("bazel.build"));

            final Project finalProjectNew = projectNew;
            projectNew.outputs().add(
                    proj -> {
                        final Collection<File> files = FileUtils.listFiles(
                                finalProjectNew.repository().toFile(),
                                FileFilterUtils.suffixFileFilter("_remote.repositories"),
                                FileFilterUtils.trueFileFilter()
                        );

                        //System.out.println(files);
                        //noinspection ResultOfMethodCallIgnored
                        files.forEach(File::delete);
                    }
            );

            // generate repository as is so generate new settings xml
        } else {
            props.put("mirroring", false);
            builder.useSnapshot(true);

            final Path m2Directory = projectNew.m2Directory();
            final Path newSettingsXml = m2Directory.resolve("settings.xml");

            final XMLDocument currentSettings = new XMLDocument(
                    new Xembler(
                            new Directives().xpath("/settings/localRepository").set(projectNew.repository())
                    ).applyQuietly(currentSettingsXml.node())
            );

            Files.copy(new InputStreamOf(new Pom.PrettyPrintXml(currentSettings)), newSettingsXml);

            projectNew.outputs().add(
                    new OutputFile.ArchiveOf(
                            new Archive.LocalRepositoryDir(bazelLocalRepository.toPath()),
                            new OutputTo(this.repositorySnapshot)
                    )
            );
        }

        final Text buildSettings = new Template.Mustache(new ResourceOf("settings.mustache.xml"), props.build()).eval();
        final String tpl = buildSettings.asString();
        log.info("builds settings.xml:\n{}", new Pom.PrettyPrintXml(currentSettingsXml).asString());

        final RunManifest runManifest = builder
                .settingsXmlTemplate(new Pom.PrettyPrintXml(new XMLDocument(tpl)).asString())
                .build();


        projectNew.outputs().add(
                new OutputFile.Content(
                        runManifest.asBinary(),
                        runManifestOutput
                )
        );
        return projectNew;
    }
}
