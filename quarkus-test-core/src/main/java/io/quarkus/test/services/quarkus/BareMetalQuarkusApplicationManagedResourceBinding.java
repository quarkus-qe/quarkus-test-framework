package io.quarkus.test.services.quarkus;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.security.certificate.QuarkusApplicationCertificateConfigurator;

public class BareMetalQuarkusApplicationManagedResourceBinding implements QuarkusApplicationManagedResourceBinding {
    @Override
    public boolean appliesFor(ServiceContext context) {
        return context.getTestContext().getRequiredTestClass().isAnnotationPresent(QuarkusScenario.class)
                // this is an ugly hack to prevent this binding to override openshift or kubernetes binding,
                // since resource builder uses only first binding that matches.
                // If app has both @QuarkusScenario and @OpenshiftScenario (due to inheritance), this one will have precence.
                // Also we don't want this binding to apply at all for OCP scenarios.
                // FIXME: It would be cleaner to implement some annotation overriding or prioritization
                && !context.getTestContext().getRequiredTestClass().getSimpleName().toLowerCase().startsWith("openshift")
                && !context.getTestContext().getRequiredTestClass().getSimpleName().toLowerCase().startsWith("kubernetes");
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
