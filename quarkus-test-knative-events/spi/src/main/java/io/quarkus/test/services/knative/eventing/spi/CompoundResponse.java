package io.quarkus.test.services.knative.eventing.spi;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;

public abstract class CompoundResponse<T> {

    private static final int TIMEOUT_SECONDS = 30;
    private final int expectedNumOfResponses;
    private final AtomicInteger actualNumOfResponses;

    protected CompoundResponse(int expectedNumOfResponses) {
        this.expectedNumOfResponses = expectedNumOfResponses;
        this.actualNumOfResponses = new AtomicInteger(0);
    }

    public final void recordVisit() {
        actualNumOfResponses.incrementAndGet();
    }

    public final void recordResponse(T response) {
        addResponse(response);
        actualNumOfResponses.incrementAndGet();
    }

    public final CompoundResponse<T> waitForResponses() {
        Awaitility.await().atMost(Duration.ofSeconds(TIMEOUT_SECONDS)).until(this::isDone);
        return this;
    }

    public final ForwardResponseDTO<T> join() {
        return new ForwardResponseDTO<>(getJoinedResponse());
    }

    protected abstract void addResponse(T response);

    protected abstract T getJoinedResponse();

    private boolean isDone() {
        return actualNumOfResponses.get() == expectedNumOfResponses;
    }

    public static final class StringCompoundResponse extends CompoundResponse<String> {

        private final StringBuffer stringBuffer = new StringBuffer();

        public StringCompoundResponse(int expectedNumOfResponses) {
            super(expectedNumOfResponses);
        }

        @Override
        protected void addResponse(String response) {
            stringBuffer.append(response);
        }

        @Override
        protected String getJoinedResponse() {
            return stringBuffer.toString();
        }
    }
}
