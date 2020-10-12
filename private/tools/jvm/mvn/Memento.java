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
}
