package io.quarkus.qe.debug;

import static io.quarkus.test.debug.SureFireDebugProvider.APP_IS_READ_PRESS_ENTER_TO_EXIT;
import static io.quarkus.test.debug.SureFireDebugProvider.RUN_TESTS;
import static io.quarkus.test.debug.SureFireDebugProvider.TEST_RUN_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;

@DisabledOnOs(WINDOWS)
@DisabledOnNative
public class SureFireDebugIT extends MojoTestBase {

    @Test
    public void smokeTest() throws Exception {
        // minimal test - start in debug mode, run tests, press enter, check tests were successful

        // run maven invoker test, use debug mode
        final File testDir = initProject("project/classic", "project/classic-result");
        final RunningInvoker invoker = new RunningInvoker(testDir, false);
        invoker.setMavenExecutable(new File("../../mvnw").getAbsoluteFile());
        final MavenProcessInvocationResult result = invoker.execute(Arrays.asList("clean", "verify", "-Dts.debug",
                "-D" + RUN_TESTS),
                Collections.emptyMap());

        // wait till app is ready for debugging and press enter
        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            if (result.getProcess() != null
                    && result.getProcess().isAlive()
                    && invoker.log().contains(APP_IS_READ_PRESS_ENTER_TO_EXIT)) {
                result.getProcess().getOutputStream();
                var writer = new BufferedWriter(new OutputStreamWriter(result.getProcess().getOutputStream()));
                writer.write(System.lineSeparator());
                writer.flush();
                writer.close();
                return true;
            }
            return false;
        });

        // wait till process finished
        await().atMost(1, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());

        // assert build and tests were successful
        assertThat(invoker.log()).containsIgnoringCase(TEST_RUN_SUCCESS);
        assertThat(invoker.log()).containsIgnoringCase("BUILD SUCCESS");

        invoker.stop();
    }

}
