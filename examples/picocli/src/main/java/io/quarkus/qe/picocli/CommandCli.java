package io.quarkus.qe.picocli;

import org.jboss.logging.Logger;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "example", mixinStandardHelpOptions = true, subcommands = { HelloWorldExample.class })
public class CommandCli {
}

@CommandLine.Command(name = "helloWorld", description = "helloWorld useCase")
class HelloWorldExample implements Runnable {

    private static final Logger LOG = Logger.getLogger(HelloWorldExample.class);

    @CommandLine.Option(names = { "-n", "--yourName" }, description = "your name", defaultValue = "World")
    String yourName;

    @Override
    public void run() {
        String result = String.format("Hello %s!", yourName);
        LOG.info(result);
    }
}
