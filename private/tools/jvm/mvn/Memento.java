package tools.jvm.mvn;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import lombok.experimental.UtilityClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

@UtilityClass
public class Memento {
    private static final Map<Object, Object> IDENT = new IdentityHashMap<>();

    interface NoExceptionSupplier<V> {
        V get() throws Exception;
    }

    static <V> Supplier<V> memorize(NoExceptionSupplier<V> act) {
        return Suppliers.memoize(() -> {
            try {
                return act.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }


    @SuppressWarnings({"UnstableApiUsage"})
    static Project memorize(Project self) {
        return (Project) IDENT.computeIfAbsent(self, (k) -> {
            final Map<Method, Object> cache = Maps.newHashMap();
            return Reflection.newProxy(Project.class, new AbstractInvocationHandler() {
                @SuppressWarnings("NullableProblems")
                @Override
                protected Object handleInvocation(Object o, Method method, Object[] args) {
                    return cache.computeIfAbsent(method, (m) -> call(args, m));
                }
                @SuppressWarnings("unchecked")
                private Object call(Object[] args, Method m) {
                    final Invokable<Project, Object> invokable = (Invokable<Project, Object>) Invokable.from(m);
                    try {
                        return invokable.invoke(self, args);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new ToolException(e);
                    }
                }
            });
        });

    }
}
