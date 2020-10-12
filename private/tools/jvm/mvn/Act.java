package tools.jvm.mvn;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.util.List;

public interface Act {

    /**
     * Execute action with project
     * @param project project
     */
    Project accept(Project project);


    /**
     * Exec one by one.
     */
    class Iterative  implements Act {
        private final List<Act> acts;

        public Iterative(Act...acts) {
            this(Lists.newArrayList(acts));
        }

        public Iterative(List<Act> acts) {
            this.acts = acts;
        }

        @Override
        public Project accept(Project project) {
            for (Act act : acts) {
                project = act.accept(project);
            }
            return project;
        }
    }

}
