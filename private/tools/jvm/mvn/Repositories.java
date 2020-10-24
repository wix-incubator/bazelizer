package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

public interface Repositories extends Iterable<Repositories.Repository> {

    String REPOSITORY_ID = "bazelizer";

    @Data
    @Accessors(fluent = true, chain = true)
    class Repository {
        private String id;
        private String name;
        private String url;

        private boolean noSnapshots = false;
    }


    @AllArgsConstructor
    class Raw implements Repositories {
        @Delegate
        private final Iterable<Repository> origin;
    }

    /**
     *
     */
    class BazelLinkedLocalM2  implements Repositories {

        @Delegate
        private final Iterable<Repository> origin;

        public BazelLinkedLocalM2(Path path) {
            origin = Optional.ofNullable(path).map(p -> Collections.singleton(
                    new Repository()
                            .id(REPOSITORY_ID)
                            .name("Host local m2 cache, linked via bazel")
                            .url(Texts.quietPathToURL(p).toString())
                    )).orElse(Collections.emptySet());
        }

    }

}
