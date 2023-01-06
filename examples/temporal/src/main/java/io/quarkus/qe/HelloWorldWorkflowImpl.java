package io.quarkus.qe;

import io.temporal.workflow.Workflow;

public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {

    FormatActivity formatActivity;

    public HelloWorldWorkflowImpl() {
        formatActivity = Workflow.newActivityStub(FormatActivity.class, FormatActivity.FORMAT_ACTIVITY_DEFAULT_OPTS);
    }

    @Override
    public String getGreeting(String name) {
        // This is the entry point to the Workflow.
        // If there were other Activity methods they would be orchestrated here or from within other Activities.
        return formatActivity.composeGreeting(name);
    }
}
