package io.quarkus.test.services.operator;

import java.lang.annotation.Annotation;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.Operator;

public class OperatorManagedResourceBuilder implements ManagedResourceBuilder {

    private ServiceContext context;
    private String name;
    private String channel;
    private String source;
    private String sourceNamespace;

    protected String getName() {
        return name;
    }

    protected String getChannel() {
        return channel;
    }

    protected String getSource() {
        return source;
    }

    protected String getSourceNamespace() {
        return sourceNamespace;
    }

    protected ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        Operator metadata = (Operator) annotation;
        name = metadata.name();
        channel = metadata.channel();
        source = metadata.source();
        sourceNamespace = metadata.sourceNamespace();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        return new OperatorManagedResource(this);
    }
}
