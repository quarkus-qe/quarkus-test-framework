package io.quarkus.test.debug;

import static io.quarkus.test.debug.SureFireCommunicationHelper.startReceiverCommunication;
import static java.lang.Boolean.parseBoolean;
import static org.apache.maven.surefire.api.suite.RunResult.noTestsRun;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusScenarioBootstrap;
import io.quarkus.test.bootstrap.TestContext;

public class SureFireDebugProvider extends AbstractProvider {

    public static final String APP_IS_READ_PRESS_ENTER_TO_EXIT = "Application is ready for debugging. Press enter to exit:";
    public static final String TEST_RUN_SUCCESS = "Run all tests without failure";
    public static final String RUN_TESTS = "ts.debug.run-tests";
    private static final Logger LOG = Logger.getLogger(SureFireDebugProvider.class);
    private static final int SLEEP_TIMEOUT = 500;
    private final ProviderParameters parameters;
    private final boolean runTests;
    private final boolean skipBeforeAndAfterTestClassMethods;

    public SureFireDebugProvider(ProviderParameters parameters) {
        this.parameters = parameters;
        String waitingStrategyProp = System.getProperty(RUN_TESTS);
        if (waitingStrategyProp == null || waitingStrategyProp.isBlank()) {
            runTests = false;
        } else {
            runTests = Boolean.parseBoolean(waitingStrategyProp);
        }
        skipBeforeAndAfterTestClassMethods = parseBoolean(System.getProperty("ts.debug.skip-before-and-after-methods"));
    }

    @Override
    public Iterable<Class<?>> getSuites() {
        return Set.of();
    }

    @Override
    public RunResult invoke(Object o) throws TestSetFailedException, ReporterException, InvocationTargetException {
        var bootstrap = new QuarkusScenarioBootstrap();
        var testContext = new TestContext.TestContextImpl(findTestClass(), Set.of());
        var testClassInstance = instantiateTestClass();

        bootstrap.beforeAll(testContext);
        invokeTestBeforeAll(testClassInstance);
        if (runTests) {
            // beforeEach methods are called before test
            runTests(testClassInstance, bootstrap, testContext);
        } else {
            invokeTestBeforeEach(testClassInstance);
            bootstrap.beforeEach(testContext, findTestMethod());
        }

        waitTillUserWishesToExit();

        if (!runTests) {
            bootstrap.afterEach();
            invokeTestAfterEach(testClassInstance);
        }
        invokeTestAfterAll(testClassInstance);
        bootstrap.afterAll();

        return noTestsRun();
    }

    private Object instantiateTestClass() throws InvocationTargetException {
        try {
            return findTestClass().getConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> findTestClass() {
        Map<String, String> classes = new HashMap<>();
        parameters.getScanResult().writeTo(classes);
        // if user specified unique class with '-Dit.test=SomeIT', then there will be one class
        // if pattern is absent or matches more than one class, we simply choose one
        return loadClass(classes.values().stream().findFirst().orElseThrow());
    }

    private String findTestMethod() {
        var test = parameters.getTestRequest().getTestListResolver().getPluginParameterTest();
        if (test != null && test.contains("#")) {
            return test.split("#")[1];
        }
        return "";
    }

    private void runTests(Object testClassInstance, QuarkusScenarioBootstrap bootstrap,
            TestContext.TestContextImpl testContext) {
        // run public methods annotated with @Test, parametrized tests are ignored
        var testMethodMatcher = new Predicate<String>() {
            final String name = findTestMethod();

            @Override
            public boolean test(String s) {
                return name.isBlank() || s.equals(name);
            }
        };
        try {
            for (Method method : testClassInstance.getClass().getMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    if (testMethodMatcher.test(method.getName()) && method.getParameterCount() == 0) {
                        try {
                            bootstrap.beforeEach(testContext, method.getName());
                            invokeTestBeforeEach(testClassInstance);

                            method.invoke(testClassInstance);

                            bootstrap.afterEach();
                            invokeTestAfterEach(testClassInstance);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException("Failed to invoke method annotated with " + Test.class.getName(), e);
                        }
                    }
                }
            }
            LOG.info(TEST_RUN_SUCCESS);
        } catch (Exception exception) {
            LOG.error("Test run failed with: ", exception);
        }
    }

    private void invokeTestBeforeEach(Object testClassInstance) {
        if (!skipBeforeAndAfterTestClassMethods) {
            invokeMethodAnnotatedWith(testClassInstance, BeforeEach.class);
        }
    }

    private void invokeTestBeforeAll(Object testClassInstance) {
        if (!skipBeforeAndAfterTestClassMethods) {
            invokeMethodAnnotatedWith(testClassInstance, BeforeAll.class);
        }
    }

    private void invokeTestAfterEach(Object testClassInstance) {
        if (!skipBeforeAndAfterTestClassMethods) {
            invokeMethodAnnotatedWith(testClassInstance, AfterEach.class);
        }
    }

    private void invokeTestAfterAll(Object testClassInstance) {
        if (!skipBeforeAndAfterTestClassMethods) {
            invokeMethodAnnotatedWith(testClassInstance, AfterAll.class);
        }
    }

    private static void waitTillUserWishesToExit() {
        var helper = startReceiverCommunication();
        LOG.info(APP_IS_READ_PRESS_ENTER_TO_EXIT);
        while (!helper.receivedExitSignal()) {
            busyWaitingForHalfOfTheSecond();
        }
        helper.closeCommunication();
    }

    private static void busyWaitingForHalfOfTheSecond() {
        try {
            Thread.sleep(SLEEP_TIMEOUT);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeMethodAnnotatedWith(Object testClassInstance, Class<? extends Annotation> annotation) {
        for (Method method : testClassInstance.getClass().getMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                try {
                    method.invoke(testClassInstance);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Failed to invoke method annotated with " + annotation.getName(), e);
                }
                break;
            }
        }
    }

}
