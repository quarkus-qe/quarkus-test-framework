package io.quarkus.test.listener;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.KnownExceptionChecker;

/**
 * Extension is masking exceptions, caused by known issues which cannot be easily disabled.
 * Originally created for <a href="https://github.com/quarkusio/quarkus/issues/38334">Broken pipe</a> issue.
 * In case that related exception is thrown, this extension will swallow it, and make test marked as skipped.
 */
public class QuarkusTestExceptionFilter implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        // if exception is caused by known issue, swallow it, otherwise propagate it
        if (KnownExceptionChecker.checkForKnownException(throwable)) {
            Log.error("Skipping test " + context.getDisplayName() + " known exception found. Printing stack trace");
            throwable.printStackTrace();
            // skip the test
            Assumptions.abort();
        } else {
            throw throwable;
        }
    }
}
