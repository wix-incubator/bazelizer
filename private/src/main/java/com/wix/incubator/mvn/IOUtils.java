package com.wix.incubator.mvn;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class IOUtils {
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

    public static long tarRepositoryRecursive(Maven env, Path out) throws IOException {
        final Path dir = env.repository;
        final Collection<Path> files = FileUtils.listFiles(
                dir.toFile(), REPOSITORY_FILES_FILTER, FileFilterUtils.directoryFileFilter() // recursive
        ).stream().map(File::toPath).collect(Collectors.toList());
        try (OutputStream os = Files.newOutputStream(out)) {
            return IOUtils.tar(files, os, dir::relativize);
        }
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
                ;
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

    public static List<String> list(Path p) throws IOException {
        try (TarArchiveInputStream is = new TarArchiveInputStream(Files.newInputStream(p, StandardOpenOption.READ))) {
            final ArrayList<String> nn = new ArrayList<>();
            for (TarArchiveEntry tarEntry; (tarEntry = is.getNextTarEntry()) != null; ) {
                nn.add(tarEntry.getName());
            }
            return nn;
        }
    }

    private IOUtils() {}
}
