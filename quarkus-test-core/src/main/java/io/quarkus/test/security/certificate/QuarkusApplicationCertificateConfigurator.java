package io.quarkus.test.security.certificate;

import static io.quarkus.test.security.certificate.ServingCertificateConfig.SERVING_CERTIFICATE_KEY;

import io.quarkus.test.bootstrap.ServiceContext;

public abstract class QuarkusApplicationCertificateConfigurator {
    public static void configureCertificates(CertificateBuilder certificateBuilder, ServiceContext context) {
        context.put(CertificateBuilder.INSTANCE_KEY, certificateBuilder);
        certificateBuilder
                .certificates()
                .forEach(certificate -> certificate
                        .configProperties()
                        .forEach(context::withTestScopeConfigProperty));
        if (certificateBuilder.servingCertificateConfig() != null) {
            context.put(SERVING_CERTIFICATE_KEY, certificateBuilder.servingCertificateConfig());
        }
    }
}
