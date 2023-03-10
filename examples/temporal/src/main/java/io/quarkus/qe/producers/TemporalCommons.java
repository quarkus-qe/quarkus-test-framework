package io.quarkus.qe.producers;

import java.util.Objects;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.util.StringUtil;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

public abstract class TemporalCommons {

    public static final String HELLO_WORLD_TASK_QUEUE = "HELLO_WORLD_TASK_QUEUE";
    protected WorkflowClientOptions clientOpts;
    @Inject
    @ConfigProperty(name = "temporal.host", defaultValue = "127.0.0.1")
    protected String temporalServerHost;
    @Inject
    @ConfigProperty(name = "temporal.port", defaultValue = "7233")
    protected Integer temporalServerPort;

    @Inject
    @ConfigProperty(name = "temporal.namespace", defaultValue = "default")
    protected String namespace;

    protected WorkflowClient createWorkflowClient(WorkflowServiceStubs service) {
        if (Objects.isNull(this.clientOpts)) {
            if (StringUtil.isNullOrEmpty(namespace)) {
                this.clientOpts = WorkflowClientOptions.newBuilder().build();
            } else {
                this.clientOpts = WorkflowClientOptions.newBuilder().setNamespace(namespace).build();
            }
        }
        return WorkflowClient.newInstance(service, this.clientOpts);
    }

    protected WorkflowServiceStubs createDefaultWorkflowServiceStubs() {
        String endpoint = String.format("%s:%s", temporalServerHost, temporalServerPort);
        return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder().setTarget(endpoint).build());
    }
}
