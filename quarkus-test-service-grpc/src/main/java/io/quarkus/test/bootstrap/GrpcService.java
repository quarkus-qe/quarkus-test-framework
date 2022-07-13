package io.quarkus.test.bootstrap;

import org.apache.commons.lang3.StringUtils;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

public class GrpcService extends RestService {

    public Channel grpcChannel() {
        return ManagedChannelBuilder.forAddress(getGrpcHost(), getGrpcPort()).usePlaintext().build();
    }

    public String getGrpcHost() {
        return getHost(Protocol.GRPC).replace("grpc://", StringUtils.EMPTY).replace("http://", StringUtils.EMPTY);
    }

    public int getGrpcPort() {
        return getPort(Protocol.GRPC);
    }
}
