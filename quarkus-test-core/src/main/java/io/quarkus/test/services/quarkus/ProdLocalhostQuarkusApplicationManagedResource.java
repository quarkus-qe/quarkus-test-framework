package io.quarkus.test.services.quarkus;

import static io.quarkus.test.utils.TestExecutionProperties.rememberThisIsCliApp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import io.quarkus.test.configuration.PropertyLookup;

public class ProdLocalhostQuarkusApplicationManagedResource extends LocalhostQuarkusApplicationManagedResource {

    private static final String JAVA = "java";
    private static final String QUARKUS_ARGS_PROPERTY_NAME = "quarkus.args";
    private static final String ENABLE_PREVIEW = "--enable-preview";
    private static final PropertyLookup JAVA_ENABLE_PREVIEW = new PropertyLookup("ts.enable-java-preview", "false");

    private final ProdQuarkusApplicationManagedResourceBuilder model;

    public ProdLocalhostQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    protected List<String> prepareCommand(List<String> systemProperties) {
        List<String> command = new LinkedList<>();
        // extract 'quarkus.args' and remove the args from system properties
        String[] cmdArgs = extractQuarkusArgs(systemProperties);
        if (model.getArtifact().getFileName().toString().endsWith(".jar")) {
            command.add(JAVA);
            if (JAVA_ENABLE_PREVIEW.getAsBoolean()) {
                command.add(ENABLE_PREVIEW);
            }
            command.addAll(systemProperties);
            var debugOptions = model.getContext().getTestContext().debugOptions();
            if (debugOptions != null && debugOptions.debug()) {
                var suspend = debugOptions.suspend() ? "y" : "n";
                command.add("-agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=y,suspend=" + suspend);
            }
            command.add("-jar");
            command.add(model.getArtifact().toAbsolutePath().toString());
        } else {
            command.add(model.getArtifact().toAbsolutePath().toString());
            command.addAll(systemProperties);
        }
        command.addAll(Arrays.asList(cmdArgs));

        return command;
    }

    private String[] extractQuarkusArgs(List<String> systemProperties) {
        String[] args = ArrayUtils.EMPTY_STRING_ARRAY;
        Iterator<String> propertiesIt = systemProperties.iterator();
        while (propertiesIt.hasNext()) {
            String property = propertiesIt.next();
            if (property.contains(QUARKUS_ARGS_PROPERTY_NAME)) {
                propertiesIt.remove();
                args = property.replace("-D" + QUARKUS_ARGS_PROPERTY_NAME + "=", "").split(" ");
                rememberThisIsCliApp(this.getContext());
                break;
            }
        }

        return args;
    }
}
