package io.quarkus.qe;

import static io.quarkus.qe.producers.TemporalCommons.HELLO_WORLD_TASK_QUEUE;
import static io.quarkus.qe.producers.TemporalWorker.WORKER_SUFFIX;
import static io.quarkus.qe.producers.TemporalWorkflow.WORKFLOW_SUFFIX;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.qe.producers.TemporalWorker;
import io.quarkus.qe.producers.TemporalWorkflow;
import io.quarkus.runtime.StartupEvent;

@Path("/workflow")
public class WorkflowResource {

    @Inject
    @Named(HELLO_WORLD_TASK_QUEUE + WORKER_SUFFIX)
    TemporalWorker worker;

    @Inject
    @Named(HELLO_WORLD_TASK_QUEUE + WORKFLOW_SUFFIX)
    TemporalWorkflow workflow;

    void onStart(@Observes StartupEvent ev) {
        worker.start();
    }

    @Path("/hello/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String helloWorld(@RestPath String name) {
        HelloWorldWorkflow helloWorldWorkflow = workflow.getStub(HelloWorldWorkflow.class);
        String greeting = helloWorldWorkflow.getGreeting(name);
        return greeting;
    }
}
