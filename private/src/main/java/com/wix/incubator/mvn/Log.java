package com.wix.incubator.mvn;

public class Log {
    public static void info(String m) {
        System.out.println("    [info]  " + m);
    }
    public static void info(MavenProject project, String m) {
        System.out.println("    [info]  {" + project + "}  " + m);
    }
}
