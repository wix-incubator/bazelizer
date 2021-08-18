package com.wix.incubator.mvn;


import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.devtools.build.runfiles.Runfiles;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.wix.incubator.mvn.Cli.BZL_COURSIER_JAR_SYS_PROP;

@SuppressWarnings("Convert2MethodRef")
public class Coursier {

    public static Coursier create() throws IOException {
        Runfiles runfiles = Cli.RUNFILES;
        final File tool = Optional.ofNullable(System.getProperty(BZL_COURSIER_JAR_SYS_PROP))
                .map((runfilesPath) -> new File(runfiles.rlocation(runfilesPath)))
                .map(file -> file.toPath().resolve("jar/downloaded.jar").toFile())
                .orElseThrow(() -> new IllegalStateException("no sys prop: " + BZL_COURSIER_JAR_SYS_PROP));

        return new Coursier(tool);
    }

    private final File tool;

    public Coursier(File tool) {
        this.tool = tool;
    }

    @SneakyThrows
    public void resolve(List<String> coords) {
        invoke(coords);
    }

    private void invoke(List<String> coords) throws IOException, InterruptedException {
        final List<String> args = Lists.newArrayList(
                getJavaBinary(),
                "-jar",
                tool.getAbsolutePath(),
                "resolve"
        );
        args.addAll(coords);
        Console.info("Executing: " + String.join(" ", args));
        final Process proc = new ProcessBuilder()
                .directory(new File("."))
                .command( args )
                .start();

        int exitCode = proc.waitFor();
        readOutput(proc);

        Preconditions.checkArgument(exitCode == 0, "exit: %s", exitCode);
    }

    private void readOutput(Process proc) {
        new BufferedReader(new InputStreamReader(proc.getInputStream())).lines()
                .forEach((line) -> {
                    Console.info(line);
                });
        new BufferedReader(new InputStreamReader(proc.getErrorStream())).lines()
                .forEach((line) -> {
                    Console.error("", line);
                });
    }

    private String getJavaBinary() {
        final File javaHome = new File(System.getProperty("java.home"));
        return javaHome.toPath().resolve("bin/java").toFile().getAbsolutePath();
    }


}
