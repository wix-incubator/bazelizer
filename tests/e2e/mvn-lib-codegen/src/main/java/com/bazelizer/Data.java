package com.bazelizer;

import com.mavenizer.examples.util.Util;
import io.codejournal.maven.xsd2java.*;

public class Data {
    public static Department newDepartment() {
        Department d = new ObjectFactory().createDepartment();
        d.setId( com.mavenizer.examples.util.Util.getRandomInt() );
        d.setName( com.mavenizer.examples.util.Util.getRandomStr() );
        return d;
    }
}