package com.wix.incubator.mvn;

import com.google.common.hash.Hashing;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class IOSupport {
    public static IOFileFilter REPOSITORY_FILES_FILTER = FileFilterUtils.and(
            FileFilterUtils.fileFileFilter(),
            // SEE: https://stackoverflow.com/questions/16866978/maven-cant-find-my-local-artifacts
            //
            //So with Maven 3.0.x, when an artifact is downloaded from a repository,
            // maven leaves a _maven.repositories file to record where the file was resolved from.
            //
            //Namely: when offline, maven 3.0.x thinks there are no repositories, so will always
            // find a mismatch against the _maven.repositories file
            FileFilterUtils.notFileFilter(
                    FileFilterUtils.prefixFileFilter("_remote.repositories")
            )
    );

    public static File newTempDirectory(String pref) throws IOException {
        final File directory = newDirectory(pref, new File("."));
        directory.deleteOnExit();
        return directory;
    }

    private static File newDirectory(String pref, File root) {
        final String baseName = UUID.randomUUID().toString();
        for (int i = 0; i < 9999; i++) {
            String name = String.format("%s%s-%d", pref != null ? pref : "", baseName, i);
            final File tmp = new File(root, name);
            if (tmp.mkdir()) {
                return tmp;
            }
        }

        throw new IllegalStateException("Failed to create directory within 10000 attempts (tried " + baseName + "0 to " + baseName + 9999 + ')');
    }

    public static long tar(Collection<Path> files, OutputStream out, Function<Path, Path> tarPath) throws IOException {
        long bytes = 0;
        try (TarArchiveOutputStream aos = new TarArchiveOutputStream(out)) {
            aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Path file : files) {
                final ArchiveEntry entry = aos.createArchiveEntry(file.toFile(), tarPath.apply(file).toString());
                aos.putArchiveEntry(entry);
                bytes += Files.copy(file, aos);
                aos.closeArchiveEntry();
            }
            aos.finish();
        }
        return bytes;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void untar(Path tar, Path dest) throws IOException {
        try (TarArchiveInputStream ais = new TarArchiveInputStream(Files.newInputStream(tar))) {
            final File destFile = dest.toFile();
            for (TarArchiveEntry tarEntry; (tarEntry = ais.getNextTarEntry()) != null; ) {
                File destPath = new File(destFile, tarEntry.getName());
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    if (!destPath.getParentFile().exists()) {
                        destPath.getParentFile().mkdirs();
                    }
                    destPath.createNewFile();
                    Files.copy(ais, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static List<String> listTar(Path p) throws IOException {
        try (TarArchiveInputStream is = new TarArchiveInputStream(Files.newInputStream(p, StandardOpenOption.READ))) {
            final ArrayList<String> nn = new ArrayList<>();
            for (TarArchiveEntry tarEntry; (tarEntry = is.getNextTarEntry()) != null; ) {
                nn.add(tarEntry.getName());
            }
            return nn;
        }
    }


    public static List<String> lisTartUnchacked(Path p)  {
        final List<String> tarFiles;
        try {
            tarFiles = listTar(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return tarFiles;
    }

    public static List<String> readLines(Path text) throws IOException {
        try (Stream<String> s = Files.lines(text)) {
            return s.map(p -> {
                String line = p.trim();
                if (line.startsWith("'") || line.startsWith("\"")) {
                    line = line.substring(1);
                }
                if (line.endsWith("'") || line.endsWith("\"")) {
                    line = line.substring(0, line.length() - 1);
                }
                return line;
            }).collect(Collectors.toList());
        }
    }

    private IOSupport() {}
}
