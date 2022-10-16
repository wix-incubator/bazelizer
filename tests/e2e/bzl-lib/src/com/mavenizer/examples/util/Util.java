package com.mavenizer.examples.util;

import java.util.*;

public class Util {
    public static String getRandomStr() {
        return "!" +  Integer.toString( new Random().nextInt(), 16 );
    }

    public static int getRandomInt() {
        return new Random().nextInt();
    }
}
