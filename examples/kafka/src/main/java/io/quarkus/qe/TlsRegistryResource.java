package io.quarkus.qe;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.logging.Log;
import io.quarkus.tls.TlsConfigurationRegistry;

@Path("tls-registry")
public class TlsRegistryResource {

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @GET
    @Path("validate-config/{tls-config-name}")
    public boolean validateNamedTlsConfig(@PathParam("tls-config-name") String tlsConfigName) {
        var namedConfig = tlsRegistry.get(tlsConfigName);
        if (namedConfig.isEmpty()) {
            Log.error("TLS config '%s' is missing".formatted(tlsConfigName));
            return false;
        }
        var truststore = namedConfig.get().getTrustStore();
        if (truststore == null) {
            Log.error("TLS config '%s' truststore is not configured".formatted(tlsConfigName));
            return false;
        }
        return true;
    }

}
