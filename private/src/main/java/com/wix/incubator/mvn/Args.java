package com.wix.incubator.mvn;

import org.apache.maven.model.Dependency;
import org.xembly.Directive;
import org.xembly.Directives;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Args {
    public final List<String> cmd;
    public final List<Dep> deps;
    public final Iterable<Map.Entry<String,String>> updates;

    public Args(List<String> cmd) {
        this(cmd, Collections.emptyList(), Collections.emptyList());
    }

    public Args(List<String> cmd, List<Dep> deps) {
        this(cmd, deps, Collections.emptyList());
    }

    public Args(List<String> cmd, List<Dep> deps, Iterable<Map.Entry<String,String>> updates) {
        this.cmd = cmd;
        this.deps = deps;
        this.updates = updates;
    }



}
