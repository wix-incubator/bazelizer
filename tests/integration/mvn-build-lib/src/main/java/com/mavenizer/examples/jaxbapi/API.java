package com.mavenizer.examples.jaxbapi;

import empns.*;
import java.util.*;
import com.mavenizer.examples.subliby.NameGen;

public class API {

    public static List<EmpRequest> load() {
        NameGen g = new NameGen();
        System.out.println("call api...");
        return Arrays.asList(
             new EmpRequest().withId(1).withName(g.getStr()),
             new EmpRequest().withId(2).withName(g.getStr())
        );
    }

}