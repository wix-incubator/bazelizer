package com.wix.incubator.mvn;

public class Logs {
    public static void info(String m) {
        System.out.println("    [info]  " + m);
    }
    public static void info(Object project, String m) {
        System.out.println("    [info]  {" + project + "}  " + m);
    }
}
