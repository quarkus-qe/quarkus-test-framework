package io.quarkus.test.services.quarkus;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class ProdLocalhostQuarkusApplicationManagedResource extends LocalhostQuarkusApplicationManagedResource {

    private static final String JAVA = "java";
    private static final String QUARKUS_ARGS_PROPERTY_NAME = "quarkus.args";

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
            command.addAll(systemProperties);
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
                break;
            }
        }

        return args;
    }
}
