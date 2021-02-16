package com.bazelizer;

import com.google.common.base.Strings;
import com.mavenizer.examples.subliby.NameGen;

public class ApiBaz {
    public void load() {
        System.out.println(new NameGen().getStr() + "" + Strings.class);
    }
}