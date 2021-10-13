package io.quarkus.test.bootstrap;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.services.URILike;

public class GrpcService extends RestService {

    public Channel grpcChannel() {
        return ManagedChannelBuilder.forAddress(getGrpcHost().getHost(), getGrpcHost().getPort()).usePlaintext().build();
    }

    public URILike getGrpcHost() {
        return getURI(Protocol.GRPC);
    }
}
