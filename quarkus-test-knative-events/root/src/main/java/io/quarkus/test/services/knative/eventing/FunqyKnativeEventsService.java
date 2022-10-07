package io.quarkus.test.services.knative.eventing;

import static io.quarkus.test.bootstrap.inject.OpenShiftClient.invokeMethod;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matcher;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.knative.eventing.v1.BrokerBuilder;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.test.bootstrap.BaseService;
import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.services.knative.eventing.spi.ForwardRequestDTO;
import io.quarkus.test.services.knative.eventing.spi.ForwardResponseDTO;
import io.quarkus.test.utils.AwaitilityUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Represents service with one or more Funqy functions within a Knative Events environment.
 */
public class FunqyKnativeEventsService extends BaseService<FunqyKnativeEventsService> {

    private static final String CLUSTER_ENTRYPOINT_PATH = "clusterEntrypoint";
    private static final String DEFAULT_BROKER_NAME = "default";
    private static final String READY = "Ready";
    private final Set<TriggerBuilder> triggerBuilders = new HashSet<>();
    private Broker broker;
    private Trigger[] triggers = null;
    private KnativeClient knativeClient;

    public FunqyKnativeEventsService() {
        super();
        createBrokerAndBuildTriggersOnPreStart();
        createTriggersOnPostStart();
        // delete brokers and triggers on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deleteTriggers();
            deleteBroker();
        }));
    }

    private void deleteBroker() {
        // FIXME: check delete result once we migrate to Quarkus 2.14 (see below)
        getKnClient().brokers().delete();
    }

    private void deleteTriggers() {
        // FIXME: check delete result once we migrate to Quarkus 2.14 (see below)
        getKnClient().triggers().delete();
    }

    private KnativeClient getKnClient() {
        if (knativeClient == null) {
            knativeClient = context.<OpenShiftClient> get(OpenShiftExtensionBootstrap.CLIENT).getKnClient();
        }
        return knativeClient;
    }

    private void buildTriggers(String serviceName) {
        triggers = triggerBuilders
                .stream()
                .map(triggerBuilder -> triggerBuilder.build(serviceName))
                .toArray(Trigger[]::new);
    }

    public FunqyKnativeEventsService withDefaultBroker() {
        return withBroker(DEFAULT_BROKER_NAME);
    }

    public FunqyKnativeEventsService withBroker(String brokerName) {

        if (broker != null) {
            throw new RuntimeException("Broker has already been defined. Only one broker is supported");
        }

        broker = new BrokerBuilder()
                .withNewMetadata()
                .withName(brokerName)
                .endMetadata()
                .build();
        return this;
    }

    public TriggerBuilder withTrigger() {
        return new TriggerBuilder(this);
    }

    private void createBrokerAndBuildTriggersOnPreStart() {
        onPreStart(service -> {

            // we can only build triggers once we know service name
            buildTriggers(getName());

            // at least one broker must be created
            if (broker == null) {
                fail(FunqyKnativeEventsService.class.getName() + " - You must configure exactly one Knative broker.");
            }

            // set broker URL reachable within cluster, so that rest client can forward our requests to broker
            withProperty("broker-url", String.format("http://broker-ingress.knative-eventing.svc.cluster.local/%s/%s",
                    getKnClient().getNamespace(), broker.getMetadata().getName()));

            // create broker
            // TODO: call directly once we migrate to Quarkus 2.14
            broker = (Broker) invokeMethod(getKnClient().brokers(), "create", broker, "create broker",
                    broker -> new Broker[] { broker });
            // wait until the broker is ready
            final AtomicBoolean isBrokerReady = new AtomicBoolean(false);
            // access events as long as the broker is not ready, or we run out of time
            try (var ignored = watchBrokerEventsTillItsReady(broker.getMetadata().getName(), isBrokerReady)) {
                AwaitilityUtils.untilIsTrue(isBrokerReady::get);
            }
        });
    }

    /**
     * Triggers must be created once Knative service is ready.
     */
    private void createTriggersOnPostStart() {
        onPostStart(service -> {
            // at least one trigger must be created
            if (triggers == null || triggers.length == 0) {
                fail(FunqyKnativeEventsService.class.getName() + " - You must configure at least one trigger.");
            }

            for (int i = 0; i < triggers.length; i++) {
                // create trigger
                // TODO: call directly once we migrate to Quarkus 2.14
                triggers[i] = (Trigger) invokeMethod(getKnClient().triggers(), "create", triggers[i], "create trigger",
                        trigger -> new Trigger[] { trigger });
                // wait until the trigger is ready
                final AtomicBoolean isTriggerReady = new AtomicBoolean(false);
                // access events as long as the trigger is not ready, or we run out of time
                try (var ignored = watchTriggerEventsTillItsReady(triggers[i].getMetadata().getName(), isTriggerReady)) {
                    AwaitilityUtils.untilIsTrue(isTriggerReady::get);
                }
            }
        });
    }

    private Watch watchBrokerEventsTillItsReady(String brokerName, AtomicBoolean isBrokerReady) {
        final var watcher = new Watcher<Broker>() {
            @Override
            public void eventReceived(Action action, Broker broker1) {
                if (isOurBroker(broker1) && hasStatus(broker1)) {
                    isBrokerReady.set(isBrokerReady(broker1));
                }
            }

            private boolean isBrokerReady(Broker broker) {
                return broker
                        .getStatus()
                        .getConditions()
                        .stream()
                        .anyMatch(condition -> READY.equals(condition.getType())
                                && Boolean.parseBoolean(condition.getStatus()));
            }

            private boolean hasStatus(Broker broker) {
                return broker.getStatus() != null && broker.getStatus().getConditions() != null
                        && !broker.getStatus().getConditions().isEmpty();
            }

            private boolean isOurBroker(Broker broker) {
                return broker != null && brokerName.equals(broker.getMetadata().getName());
            }

            @Override
            public void onClose(WatcherException e) {
                fail("Broker '%s' state can't be retrieved.", e);
            }
        };
        // TODO: call directly once we migrate to Quarkus 2.14
        return (Watch) invokeMethod(getKnClient().brokers(), "watch", watcher, "check broker status", w -> {
            throw new IllegalStateException("We don't support generic array conversion for watcher yet.");
        });
    }

    private Watch watchTriggerEventsTillItsReady(String triggerName, AtomicBoolean isTriggerReady) {
        final var watcher = new Watcher<Trigger>() {
            @Override
            public void eventReceived(Action action, Trigger trigger) {
                if (isOurTrigger(trigger) && hasStatus(trigger)) {
                    isTriggerReady.set(isTriggerReady(trigger));
                }
            }

            private boolean isTriggerReady(Trigger trigger) {
                return trigger
                        .getStatus()
                        .getConditions()
                        .stream()
                        .anyMatch(condition -> READY.equals(condition.getType())
                                && Boolean.parseBoolean(condition.getStatus()));
            }

            private boolean hasStatus(Trigger trigger) {
                return trigger.getStatus() != null && trigger.getStatus().getConditions() != null
                        && !trigger.getStatus().getConditions().isEmpty();
            }

            private boolean isOurTrigger(Trigger trigger) {
                return trigger != null && triggerName.equals(trigger.getMetadata().getName());
            }

            @Override
            public void onClose(WatcherException e) {
                fail("Trigger '%s' state can't be retrieved.", e);
            }
        };
        // TODO: call directly once we migrate to Quarkus 2.14
        return (Watch) invokeMethod(getKnClient().triggers(), "watch", watcher, "check trigger status", w -> {
            throw new IllegalStateException("We don't support generic array conversion for watcher yet.");
        });
    }

    /**
     * Directly invokes Funqy function 'clusterEntrypoint' that forwards payload and headers to the broker and
     * returns response.
     */
    public <T> FuncInvoker<T> funcInvoker() {
        return new FuncInvoker<>(getURI().getRestAssuredStyleUri());
    }

    public interface ForwardResponseValidator<T> {
        ForwardResponseValidator<T> assertBody(Matcher<T> matcher);

        Response getResponse();
    }

    public static final class FuncInvoker<T> {

        private static final String APPLICATION_CLOUD_EVENTS_PLUS_JSON = "application/cloudevents+json";
        private final RequestSpecification request;
        private String cloudEventType = null;
        private String path = "";
        private T data = null;

        private FuncInvoker(String baseUrl) {
            request = RestAssured
                    .given()
                    .baseUri(requireNonNull(baseUrl));
        }

        public FuncInvoker<T> appJsonContentType() {
            request.contentType(ContentType.JSON);
            return this;
        }

        public FuncInvoker<T> appCloudEventsPlusJsonContentType() {
            requireClouedEventType();
            requireNonNull(data, "Please set property 'data' first.");
            path = "";
            request.contentType(APPLICATION_CLOUD_EVENTS_PLUS_JSON);
            request.body(new CloudEventData<>(data, cloudEventType));
            return this;
        }

        public FuncInvoker<T> cloudEventType(String cloudEventType) {
            this.cloudEventType = cloudEventType;
            path = CLUSTER_ENTRYPOINT_PATH;
            return this;
        }

        public FuncInvoker<T> data(T data) {
            request.body(new ForwardRequestDTO<>(data, requireNonNull(cloudEventType)));
            this.data = data;
            return this;
        }

        /**
         * Function will be invoked with a CloudEvent object.
         * Can't be used together with {@link #APPLICATION_CLOUD_EVENTS_PLUS_JSON}.
         */
        public FuncInvoker<T> asCloudEventObject() {

            // helps to determine proper path
            requireClouedEventType();
            path = "";

            request
                    .header("ce-specversion", "1.0")
                    .header("ce-id", UUID.randomUUID().toString())
                    .header("ce-type", CLUSTER_ENTRYPOINT_PATH)
                    .header("ce-source", "test");
            return this;
        }

        private void requireClouedEventType() {
            requireNonNull(cloudEventType, "Please set 'cloudEventType' first.");
        }

        public ForwardResponseValidator<T> post() {
            return validate(request.post(path));
        }

        public ForwardResponseValidator<T> get() {
            return validate(request.get(path));
        }

        private ForwardResponseValidator<T> validate(Response response) {
            if (response.statusCode() == HttpStatus.NOT_FOUND_404) {
                // We need Funqy function that forward cloud events to the broker. Brokers are internal by design.
                // We need a way to send events to the broker. We could expose another service, or use 'DomainMapping', but
                // that's less efficient than using existing app. 'clusterEndpoint' is a Funqy function that we call directly.
                throw new IllegalStateException("Cluster endpoint is missing. Please expose Funqy function 'clusterEntrypoint'"
                        + " that forwards messages to the broker.");
            }
            return new ForwardResponseValidator<T>() {
                @Override
                public ForwardResponseValidator<T> assertBody(Matcher<T> matcher) {
                    assertTrue(matcher.matches(response.as(ForwardResponseDTO.class).getResponse()));
                    return this;
                }

                public Response getResponse() {
                    return response;
                }
            };
        }

        private static final class CloudEventData<T> {
            private final ForwardRequestDTO<T> data;

            CloudEventData(T data, String cloudEventType) {
                this.data = new ForwardRequestDTO<>(data, cloudEventType);
            }

            public String getId() {
                return UUID.randomUUID() + "";
            }

            public String getSpecversion() {
                return "1.0";
            }

            public String getSource() {
                return "test";
            }

            public String getType() {
                return CLUSTER_ENTRYPOINT_PATH;
            }

            public ForwardRequestDTO<T> getData() {
                return data;
            }

            public String getDatacontenttype() {
                return "application/json";
            }
        }
    }

    public static final class TriggerBuilder {

        private String name = null;
        private String broker = null;
        private String filterCloudEventType = null;

        private final FunqyKnativeEventsService service;

        private TriggerBuilder(FunqyKnativeEventsService service) {
            this.service = service;
        }

        public TriggerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TriggerBuilder filterCloudEventType(String filterCloudEventType) {
            this.filterCloudEventType = filterCloudEventType;
            return this;
        }

        public TriggerBuilder defaultBroker() {
            return broker(DEFAULT_BROKER_NAME);
        }

        public TriggerBuilder broker(String broker) {
            this.broker = broker;
            return this;
        }

        public FunqyKnativeEventsService endTrigger() {
            service.triggerBuilders.add(this);
            return service;
        }

        private Trigger build(String serviceName) {

            // build trigger
            return new io.fabric8.knative.eventing.v1.TriggerBuilder()
                    .withNewMetadata()
                    .withName(name.toLowerCase())
                    .endMetadata()
                    .withNewSpec()
                    .withBroker(broker)
                    .withNewFilter()
                    .addToAttributes("type", filterCloudEventType)
                    .endFilter()
                    .withNewSubscriber()
                    .withNewRef()
                    .withApiVersion("v1")
                    .withKind("Service")
                    .withName(serviceName)
                    .endRef()
                    .endSubscriber()
                    .endSpec()
                    .build();
        }
    }

}
