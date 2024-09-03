package io.quarkus.qe.grpc;

import jakarta.inject.Inject;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.CurrentIdentityAssociation;

@GrpcService
public class HelloService extends GreeterGrpc.GreeterImplBase {

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        String message = "Hello " + name;
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sayHi(HiRequest request, StreamObserver<HiReply> responseObserver) {
        identityAssociation.getDeferredIdentity().subscribe().with(identity -> {
            String name = request.getName();
            String message = "Hello " + name;
            String principalName = identity.isAnonymous() ? "" : identity.getPrincipal().getName();
            responseObserver.onNext(HiReply.newBuilder().setMessage(message).setPrincipalName(principalName).build());
            responseObserver.onCompleted();
        });
    }
}
