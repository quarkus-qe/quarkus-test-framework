package io.quarkus.test.services.quarkus;

import java.util.LinkedList;
import java.util.List;

public class ProdLocalhostQuarkusApplicationManagedResource extends LocalhostQuarkusApplicationManagedResource {

    private static final String JAVA = "java";

    private final ProdQuarkusApplicationManagedResourceBuilder model;

    public ProdLocalhostQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    protected List<String> prepareCommand(List<String> systemProperties) {
        List<String> command = new LinkedList<>();
        if (model.getArtifact().getFileName().toString().endsWith(".jar")) {
            command.add(JAVA);
            command.addAll(systemProperties);
            command.add("-jar");
            command.add(model.getArtifact().toAbsolutePath().toString());
        } else {
            command.add(model.getArtifact().toAbsolutePath().toString());
            command.addAll(systemProperties);
        }

        return command;
    }

}
