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
    class Iterative extends Memento implements Act {

        public Iterative(Act...acts) {
            this(Lists.newArrayList(acts));
        }

        public Iterative(List<Act> acts) {
            super(project -> {
                for (Act act : acts) {
                    project = act.accept(project);
                }
                return project;
            });
        }
    }


    @AllArgsConstructor
    class Memento implements Act {
        private final Act act;

        @Override
        public Project accept(Project project) {
            Project p = Project.memento(project);
            return act.accept(p);
        }
    }

}
