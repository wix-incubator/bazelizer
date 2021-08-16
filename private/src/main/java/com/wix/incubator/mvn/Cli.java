package com.wix.incubator.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.wix.incubator.mvn.IOSupport.readLines;
import static java.util.Arrays.asList;


public class Cli {
    static {
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Cmd()).execute(args);
        System.exit(exitCode);
    }

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Path.class, (JsonDeserializer<Path>) (json, typeOfT, context) -> {
                final String asString = json.getAsString();
                return Paths.get(asString);
            }).create();

    public static final MustacheFactory MUSTACHE = new DefaultMustacheFactory();
}