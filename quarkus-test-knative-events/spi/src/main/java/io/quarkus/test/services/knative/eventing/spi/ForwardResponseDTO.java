package io.quarkus.test.services.knative.eventing.spi;

public class ForwardResponseDTO<T> {

    private T response;

    public ForwardResponseDTO(T response) {
        this.response = response;
    }

    public ForwardResponseDTO() {

    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "ForwardResponseDTO{"
                + "response=" + response
                + '}';
    }

}
