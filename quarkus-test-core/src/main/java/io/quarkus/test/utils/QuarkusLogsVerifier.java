package io.quarkus.test.utils;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.Service;

public class QuarkusLogsVerifier {

    private static final String INSTALLED_FEATURES = "Installed features";
    private static final String OPEN_TAG = "[";
    private static final String CLOSE_TAG = "]";
    private static final String COMMA = ",";

    private final Service service;

    public QuarkusLogsVerifier(Service service) {
        this.service = service;
    }

    public List<String> installedFeatures() {
        return service.getLogs().stream()
                .filter(log -> log.contains(INSTALLED_FEATURES))
                .flatMap(log -> Stream.of(StringUtils.substringBetween(log, OPEN_TAG, CLOSE_TAG).split(COMMA)))
                .map(String::trim)
                .toList();
    }
}
