package io.quarkus.test.debug;

import static io.quarkus.test.debug.SureFireCommunicationHelper.startSenderCommunication;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.extensions.ForkChannel;
import org.apache.maven.surefire.extensions.ForkNodeFactory;

public class SureFireDebugForkNodeFactory implements ForkNodeFactory {

    private static final Logger LOG = Logger.getLogger(SureFireDebugForkNodeFactory.class.getName());

    @Override
    public ForkChannel createForkChannel(ForkNodeArguments forkNodeArguments) throws IOException {
        listenToUserInputAndExitOnDemand();
        return new SurefireForkNodeFactory().createForkChannel(forkNodeArguments);
    }

    private static void listenToUserInputAndExitOnDemand() {
        // I didn't find a way to read terminal STD IN from fork node, so let's do it here
        // didn't find way to create SHUTDOWN command or send event from here, so let's just create
        // temp file; the existence of our file will tell SureFireDebugProvider to sweep environment and exit
        final var helper = startSenderCommunication();
        new Thread(new Runnable() {
            @Override
            public void run() {
                waitTillUserWishesToExit();
                helper.sendExitSignal();
            }

            private void waitTillUserWishesToExit() {
                new Scanner(System.in).nextLine();
                LOG.info("Detected exit signal, going to sweep environment and exit");
            }
        }).start();
    }
}
