package io.quarkus.test.services.knative.eventing.spi;

public class ForwardRequestDTO<T> {

    private T data;

    private String filterCloudEventType;

    public ForwardRequestDTO(T data, String filterCloudEventType) {
        this.data = data;
        this.filterCloudEventType = filterCloudEventType;
    }

    public ForwardRequestDTO() {

    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getFilterCloudEventType() {
        return filterCloudEventType;
    }

    public void setFilterCloudEventType(String filterCloudEventType) {
        this.filterCloudEventType = filterCloudEventType;
    }

    @Override
    public String toString() {
        return "ForwardDTO{"
                + " data='" + data + '\''
                + ", filterCloudEventType='" + filterCloudEventType + '\''
                + '}';
    }

}
