package com.wix.incubator.mvn;

import org.xembly.Directive;

import java.util.Collections;
import java.util.List;

public class Args {
    public final List<String> cmd;
    public final List<Dep> deps;
    public final Iterable<Directive> updates;

    public Args(List<String> cmd) {
        this(cmd, Collections.emptyList(), Collections.emptyList());
    }

    public Args(List<String> cmd, List<Dep> deps) {
        this(cmd, deps, Collections.emptyList());
    }

    public Args(List<String> cmd, List<Dep> deps, Iterable<Directive> updates) {
        this.cmd = cmd;
        this.deps = deps;
        this.updates = updates;
    }
}
