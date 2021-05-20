package io.quarkus.test.services.quarkus;

import java.util.LinkedList;
import java.util.List;

import io.quarkus.utilities.JavaBinFinder;

public class ProdLocalhostQuarkusApplicationManagedResource extends LocalhostQuarkusApplicationManagedResource {

    private final ProdQuarkusApplicationManagedResourceBuilder model;

    public ProdLocalhostQuarkusApplicationManagedResource(ProdQuarkusApplicationManagedResourceBuilder model) {
        super(model);
        this.model = model;
    }

    protected List<String> prepareCommand(List<String> systemProperties) {
        List<String> command = new LinkedList<>();
        if (model.getArtifact().getFileName().toString().endsWith(".jar")) {
            command.add(JavaBinFinder.findBin());
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
