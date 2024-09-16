package io.quarkus.test.bootstrap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.ExtensionContext;

public interface TestContext {

    Class<?> getRequiredTestClass();

    Set<String> getTags();

    TestStore getTestStore();

    Optional<String> getRunningTestMethodName();

    DebugOptions debugOptions();

    interface TestStore {

        Object get(Object key);

        void put(Object key, Object val);

    }

    record DebugOptions(boolean debug, boolean suspend) {
    }

    final class TestContextImpl implements TestContext {

        private final TestStore testStore;
        private final Class<?> requiredTestClass;
        private final Set<String> tags;
        private final String runningTestMethodName;
        private final DebugOptions debugOptions;

        public TestContextImpl(Class<?> requiredTestClass, Set<String> tags, DebugOptions debugOptions) {
            this.testStore = new MapBackedTestStore();
            this.requiredTestClass = requiredTestClass;
            this.tags = tags;
            this.runningTestMethodName = null;
            this.debugOptions = debugOptions;
        }

        TestContextImpl(Class<?> requiredTestClass, Set<String> tags, ExtensionContext.Store store) {
            this.testStore = new TestStore() {
                @Override
                public Object get(Object key) {
                    return store.get(key);
                }

                @Override
                public void put(Object key, Object val) {
                    store.put(key, val);
                }
            };
            this.requiredTestClass = requiredTestClass;
            this.tags = tags;
            this.runningTestMethodName = null;
            this.debugOptions = null;
        }

        TestContextImpl(TestContext testContext, String runningTestMethodName) {
            this.testStore = testContext.getTestStore();
            this.requiredTestClass = testContext.getRequiredTestClass();
            this.tags = testContext.getTags();
            this.runningTestMethodName = runningTestMethodName;
            this.debugOptions = null;
        }

        @Override
        public Class<?> getRequiredTestClass() {
            return requiredTestClass;
        }

        @Override
        public Set<String> getTags() {
            return tags;
        }

        @Override
        public TestStore getTestStore() {
            return testStore;
        }

        @Override
        public Optional<String> getRunningTestMethodName() {
            return runningTestMethodName == null ? Optional.empty() : Optional.of(runningTestMethodName);
        }

        @Override
        public DebugOptions debugOptions() {
            return debugOptions;
        }
    }

    class MapBackedTestStore implements TestStore {

        private final Map<Object, Object> keyToVal = new ConcurrentHashMap<>();

        @Override
        public Object get(Object key) {
            return keyToVal.get(key);
        }

        @Override
        public void put(Object key, Object val) {
            keyToVal.put(key, val);
        }
    }
}
