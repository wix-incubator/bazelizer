package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.Output;

import java.nio.file.Path;

@AllArgsConstructor
public class Snapshot {
    private final Path m2HomeDir;


    public Archive output() {
        final IOFileFilter repositoryFiles = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.notFileFilter(
                    FileFilterUtils.or(
                            // SEE: https://stackoverflow.com/questions/16866978/maven-cant-find-my-local-artifacts
                            //
                            //So with Maven 3.0.x, when an artifact is downloaded from a repository,
                            // maven leaves a _maven.repositories file to record where the file was resolved from.
                            //
                            //Namely: when offline, maven 3.0.x thinks there are no repositories, so will always
                            // find a mismatch against the _maven.repositories file
                            FileFilterUtils.prefixFileFilter("_remote.repositories"),

                            // exclude original settings xml
                            FileFilterUtils.prefixFileFilter("settings.xml")
                    )
                )
        );
        return new Archive.TarDirectory(m2HomeDir, repositoryFiles);
    }
}
