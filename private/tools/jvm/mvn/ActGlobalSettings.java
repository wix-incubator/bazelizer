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
import org.cactoos.io.InputOf;
import org.cactoos.io.InputStreamOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.ResourceOf;
import org.xembly.Directives;
import org.xembly.Xembler;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

@Slf4j
@AllArgsConstructor
public class ActGlobalSettings implements Act {

    /**
     * Ctor.
     * @param settings settings xml
     * @param outputManifest output build run manifest.
     */
    public ActGlobalSettings(Path settings, Path outputManifest) {
        this(new InputOf(settings), new OutputTo(outputManifest));
    }

    /**
     * Current build manifest.
     */
    private final Input globalSettingsXml;

    /**
     * Output run manifest {@link RunManifest}
     */
    private final Output runManifestOutput;

    @SneakyThrows
    @Override
    public Project accept(Project project) {

        final XML currentSettingsXml = new XMLDocument(
                new InputStreamOf(globalSettingsXml)
        );

        final File bazelLocalRepository = new File(
                currentSettingsXml.xpath("/settings/localRepository/text()").get(0)
        );

        log.info("settings.xml:\n{}", new Pom.PrettyPrintXml(currentSettingsXml).asString());

        final String tpl = new Template.Mustache(
                new ResourceOf("settings.mustache.xml"),
                ImmutableMap.of(
                        "localRepository", "{{ localRepository }}",
                        "mirrorUrl", bazelLocalRepository.toURI().toString(),
                        "mirrorId", SysProps.workspace().orElse("bazel.build")
                )
        ).eval().asString();

        final XML buildSettingsXmlTpl = new XMLDocument(
                tpl
        );

        final RunManifest runManifest = new RunManifest.Builder()
                .settingsXmlTemplate(new Pom.PrettyPrintXml(buildSettingsXmlTpl).asString())
                .build();

        final Project projectNew = project.toBuilder()
                .m2Directory(bazelLocalRepository.toPath().getParent())
                .build();

        projectNew.outputs().add(
                new OutputFile.Content(
                        runManifest.asBinary(),
                        runManifestOutput
                )
        );

        projectNew.outputs().add(
                proj -> {
                    final Collection<File> files = FileUtils.listFiles(
                            projectNew.repository().toFile(),
                            FileFilterUtils.suffixFileFilter("_remote.repositories"),
                            FileFilterUtils.trueFileFilter()
                    );

                    //System.out.println(files);
                    //noinspection ResultOfMethodCallIgnored
                    files.forEach(File::delete);
                }
        );
        return projectNew;
    }
}
