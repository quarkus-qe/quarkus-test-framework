package io.quarkus.qe;

import static io.quarkus.qe.producers.TemporalCommons.HELLO_WORLD_TASK_QUEUE;
import static io.quarkus.qe.producers.TemporalWorker.WORKER_SUFFIX;
import static io.quarkus.qe.producers.TemporalWorkflow.WORKFLOW_SUFFIX;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
