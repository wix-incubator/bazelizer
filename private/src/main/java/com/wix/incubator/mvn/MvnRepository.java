package com.wix.incubator.mvn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class MvnRepository {
    public String id;
    public String url;

    public static List<MvnRepository> fromFile(Path file) throws IOException {
        return IOSupport.readLines(file).stream()
                .map(json -> Cli.GSON.fromJson(json, MvnRepository.class))
                .collect(Collectors.toList());
    }
}
