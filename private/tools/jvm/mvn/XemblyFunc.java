package tools.jvm.mvn;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.jcabi.xml.XML;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.SyntaxException;

import javax.xml.xpath.XPathFactory;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

public interface XemblyFunc {

    Iterable<Directive> dirs(Project project, XML xml);


    class PomDefaultStruc implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            return new Directives().xpath("/project").addIf("dependencies");
        }
    }

    class PomParentRelPath implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            return new Directives()
                    .xpath("/project/parent")
                    .addIf("relativePath").set(project.toView().parent());
        }
    }

    class PomDropDeps implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            return new Directives().xpath("/project/dependencies/dependency").remove();
        }
    }
}
