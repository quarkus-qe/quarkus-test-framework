package io.quarkus.test.bootstrap;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

public final class CloseableManagedChannel extends ManagedChannel implements Closeable {

    private final ManagedChannel channel;

    public CloseableManagedChannel(ManagedChannel channel) {
        this.channel = channel;
    }

    @Override
    public ManagedChannel shutdown() {
        return channel.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return channel.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return channel.isTerminated();
    }

    @Override
    public ManagedChannel shutdownNow() {
        return channel.shutdownNow();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return channel.awaitTermination(l, timeUnit);
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor,
            CallOptions callOptions) {
        return channel.newCall(methodDescriptor, callOptions);
    }

    @Override
    public String authority() {
        return channel.authority();
    }

    @Override
    public void close() {
        channel.shutdownNow();
    }
}
