package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.security.certificate.QuarkusApplicationCertificateConfigurator;

public class BareMetalQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {
    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(QuarkusScenario.class);
    }

    @Override
    public QuarkusManagedResource init(ProdQuarkusApplicationManagedResourceBuilder builder) {
        if (builder.certificateBuilder != null) {
            QuarkusApplicationCertificateConfigurator.configureCertificates(
                    builder.certificateBuilder, builder.getContext());
        }

        return new ProdLocalhostQuarkusApplicationManagedResource(builder);
    }
}
