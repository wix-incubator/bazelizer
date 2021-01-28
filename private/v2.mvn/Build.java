package tools.jvm.v2.mvn;

public abstract class Build {

    public abstract void exec();

    public abstract void addDeps(Iterable<Dep> deps);
}
