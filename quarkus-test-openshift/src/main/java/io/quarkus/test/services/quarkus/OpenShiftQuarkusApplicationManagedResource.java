package io.quarkus.test.services.quarkus;

import static io.quarkus.test.bootstrap.inject.OpenShiftClient.TLS_ROUTE_SUFFIX;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.CA_BUNDLE_CONFIGMAP_NAME;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.EXTERNAL_SSL_PORT;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.SERVING_CERTS_SECRET_NAME;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.buildAnnotatedConfigMapProp;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.buildAnnotationProp;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.buildMountConfigMapProp;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.buildMountSecretProp;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.checkPodReadinessWithStatusInsteadOfRoute;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.createAnnotatedConfigMapPropertyKey;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.createAnnotationPropertyKey;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.createConfigMapPropertyKey;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.createSecretPropertyKey;
import static io.quarkus.test.openshift.utils.OpenShiftPropertiesUtils.getInternalServiceHttpsUrl;
import static io.quarkus.test.security.certificate.ServingCertificateConfig.isServingCertificateScenario;
import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings;
import static io.quarkus.test.utils.AwaitilityUtils.untilIsNotNull;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.OpenShiftExtensionBootstrap;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.logging.OpenShiftLoggingHandler;
import io.quarkus.test.security.certificate.ServingCertificateConfig;
import io.quarkus.test.services.URILike;

public abstract class OpenShiftQuarkusApplicationManagedResource<T extends QuarkusApplicationManagedResourceBuilder>
        extends QuarkusManagedResource {

    private static final int EXTERNAL_PORT = 80;

    protected final T model;
    protected final OpenShiftClient client;

    private LoggingHandler loggingHandler;
    private boolean init;
    private boolean running;

    private Map<Protocol, URILike> uri = new HashMap<>();

    public OpenShiftQuarkusApplicationManagedResource(T model) {
        super(model.getContext());
        this.model = model;
        this.client = model.getContext().get(OpenShiftExtensionBootstrap.CLIENT);
        configureServingCertificates();
    }

    protected abstract void doInit();

    protected abstract void doUpdate();

    @Override
    public void start() {
        if (running) {
            return;
        }

        if (!init) {
            doInit();
            init = true;
        } else {
            doUpdate();
        }

        client.scaleTo(model.getContext().getOwner(), 1);

        running = true;

        loggingHandler = new OpenShiftLoggingHandler(model.getContext());
        loggingHandler.startWatching();
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        client.scaleTo(model.getContext().getOwner(), 0);
        uri.clear();
        running = false;
    }

    @Override
    public URILike getURI(Protocol protocol) {
        final ServiceContext context = model.getContext();
        final boolean isServerless = client.isServerlessService(context.getName());
        final boolean isServingCertSslScenario = isServingCertificateScenario(context) && model.isSslEnabled();
        final boolean isTlsSecuredProtocol = (protocol == Protocol.HTTPS || protocol == Protocol.WSS);
        if (isTlsSecuredProtocol && !model.isSslEnabled() && !isServerless && !isServingCertSslScenario) {
            fail("SSL is not enabled");
        } else if (protocol == Protocol.GRPC) {
            fail("gRPC is not supported for OpenShift tests yet");
        } else if (protocol == Protocol.MANAGEMENT && model.useSeparateManagementInterface()) {
            if (model.useManagementSsl()) {
                fail("SSL is not supported for management on OpenShift tests yet");
            }
            return client.url(context.getOwner().getName() + "-management").withPort(EXTERNAL_PORT);
        }

        // service scenarios only works on HTTPS and also on some places are rewriting HTTP to HTTPS
        // but framework has HTTP hardcoded on some places, so redirect HTTP to HTTPS
        if (isServingCertSslScenario && protocol == Protocol.HTTP) {
            protocol = Protocol.HTTPS;
        }

        if (!this.uri.containsKey(protocol)) {
            if (isServingCertSslScenario) {
                this.uri.put(protocol, getInternalServiceHttpsUrl(context));
                return this.uri.get(protocol);
            }
            final int port = (isServerless || isTlsSecuredProtocol) ? EXTERNAL_SSL_PORT : EXTERNAL_PORT;
            String serviceName = isTlsSecuredProtocol && !isServerless
                    ? context.getOwner().getName() + TLS_ROUTE_SUFFIX
                    : context.getOwner().getName();
            URILike uri = untilIsNotNull(
                    () -> client.url(serviceName).withPort(port),
                    AwaitilitySettings.defaults().withService(getContext().getOwner()));
            this.uri.put(protocol, uri);
        }
        if (isServingCertSslScenario) {
            if (uriHasNotHttpsProtocol()) {
                Assertions.fail("Certificate serving scenario must use HTTPS protocol, but got " + this.uri);
            }
            return this.uri.get(protocol);
        } else if (isServerless) {
            // serverless uses internal URL and it must always use HTTPS protocol no matter of what bare-metal tests
            if (uriHasNotHttpsProtocol()) {
                Assertions.fail("Serverless scenarios must always use HTTPS protocol, but got " + this.uri);
            }
            return this.uri.get(protocol);
        } else if (Protocol.WS == protocol || Protocol.HTTP == protocol
                || isTlsSecuredProtocol) {
            // grpc is unreachable at this point
            return this.uri.get(protocol).withScheme(protocol.getValue());
        } else if (Protocol.MANAGEMENT == protocol) {
            // management when not on a separate interface must be HTTP url same as any application endpoint
            return this.uri.get(protocol).withScheme(Protocol.HTTP.getValue());
        }
        // protocol NONE has no scheme
        return this.uri.get(protocol);
    }

    private boolean uriHasNotHttpsProtocol() {
        return this.uri.values().stream().anyMatch(uri -> uri.getScheme().equals("http"));
    }

    public boolean isRunning() {
        if (!running) {
            return false;
        }

        if (client.isServerlessService(model.getContext().getName())) {
            return routeIsReachable(Protocol.HTTPS);
        }

        if (checkPodReadinessWithStatusInsteadOfRoute(model.getContext())) {
            var serviceName = model.getContext().getOwner().getName();
            return super.isRunning() && client.isAnyServicePodReady(serviceName);
        } else {
            return super.isRunning() && routeIsReachable(Protocol.HTTP);
        }
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    @Override
    public void restart() {
        stop();
        if (model.buildPropertiesChanged()) {
            init = false;
            model.build();
        }

        start();
    }

    @Override
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    private boolean routeIsReachable(Protocol protocol) {
        var url = getURI(protocol);
        return given().relaxedHTTPSValidation().baseUri(url.getRestAssuredStyleUri()).basePath("/").port(url.getPort())
                .redirects().follow(false).get().getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE;
    }

    private void configureServingCertificates() {
        // this is based on https://quarkus.io/guides/tls-registry-reference#utilizing-openshift-serving-certificates
        var ctx = model.getContext();
        if (isServingCertificateScenario(ctx)) {
            var config = ServingCertificateConfig.get(ctx);
            if (config.addServiceCertificate()) {
                // add service annotation
                var annotationVal = buildAnnotationProp("service.beta.openshift.io/serving-cert-secret-name",
                        SERVING_CERTS_SECRET_NAME);
                var annotationKey = createAnnotationPropertyKey();
                ctx.withTestScopeConfigProperty(annotationKey, annotationVal);
                // mount secret created by OpenShift operator
                var mountSecretVal = buildMountSecretProp(SERVING_CERTS_SECRET_NAME, "/etc/tls");
                var mountSecretKey = createSecretPropertyKey();
                ctx.withTestScopeConfigProperty(mountSecretKey, mountSecretVal);
                if (!config.useKeyStoreProvider()) {
                    // configure TLS registry with mounted secret
                    if (config.tlsConfigName() == null) {
                        ctx.withTestScopeConfigProperty("quarkus.tls.key-store.pem.acme.cert", "/etc/tls/tls.crt");
                        ctx.withTestScopeConfigProperty("quarkus.tls.key-store.pem.acme.key", "/etc/tls/tls.key");
                    } else {
                        ctx.withTestScopeConfigProperty("quarkus.tls." + config.tlsConfigName() + ".key-store.pem.acme.cert",
                                "/etc/tls/tls.crt");
                        ctx.withTestScopeConfigProperty("quarkus.tls." + config.tlsConfigName() + ".key-store.pem.acme.key",
                                "/etc/tls/tls.key");
                        ctx.withTestScopeConfigProperty("quarkus.http.tls-configuration-name", config.tlsConfigName());
                    }
                }
            }
            if (config.injectCABundle()) {
                var annotationVal = "true";
                var annotationKey = "service.beta.openshift.io/inject-cabundle";
                var annotatedConfigMapVal = buildAnnotatedConfigMapProp(CA_BUNDLE_CONFIGMAP_NAME, annotationKey, annotationVal);
                var annotatedConfigMapKey = createAnnotatedConfigMapPropertyKey();
                ctx.withTestScopeConfigProperty(annotatedConfigMapKey, annotatedConfigMapVal);
                var caCertDirPath = "/deployments/tls";
                var mountConfigMapVal = buildMountConfigMapProp(CA_BUNDLE_CONFIGMAP_NAME, caCertDirPath);
                var mountConfigMapKey = createConfigMapPropertyKey();
                ctx.withTestScopeConfigProperty(mountConfigMapKey, mountConfigMapVal);
                // configure TLS registry with mounted configmap
                var caCertPath = caCertDirPath + "/service-ca.crt";
                if (config.tlsConfigName() == null) {
                    ctx.withTestScopeConfigProperty("quarkus.tls.trust-store.pem.certs", caCertPath);
                } else {
                    ctx.withTestScopeConfigProperty("quarkus.tls." + config.tlsConfigName() + ".trust-store.pem.certs",
                            caCertPath);
                }
            }
        }
    }
}
