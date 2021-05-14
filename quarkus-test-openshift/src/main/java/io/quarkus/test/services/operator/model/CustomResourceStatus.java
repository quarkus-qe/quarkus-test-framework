package io.quarkus.test.services.operator.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomResourceStatus {
    private List<CustomResourceStatusCondition> conditions = new ArrayList<>();

    public List<CustomResourceStatusCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<CustomResourceStatusCondition> conditions) {
        this.conditions = conditions;
    }
}
