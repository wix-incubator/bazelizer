package tools.jvm.mvn;

import org.xembly.Directives;

/**
 * Template action, that performe modification of an xml.
 */
public interface XePom {

    /**
     * Apply directives.
     * @return directives
     */
    Directives value();


    class DependenciesTag implements XePom {

        public Directives value() {
            return new Directives()
                    .xpath("/project")
                    .addIf("dependencies");
        }
    }


    class RemoveTags implements XePom {
        @Override
        public Directives value() {
            return new Directives()
                    .xpath("/project/dependencies/dependency[not(@xe:keep) or not(string-length(@xe:keep))]")
                    .remove();
        }
    }

}
