package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLUGIN_VERSION;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.utils.GitUtils;
import io.quarkus.test.utils.MavenUtils;

public final class GitRepositoryResourceBuilderUtils {

    private static final String QUARKUS_VERSION = "quarkus.version";
    private static final String QUARKUS_PLUGIN_VERSION = "quarkus-plugin.version";
    private static final String QUARKUS_VERSION_VALUE = "${quarkus.platform.version}";

    private GitRepositoryResourceBuilderUtils() {

    }

    public static void cloneRepository(final GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        GitUtils.cloneRepository(model.getContext(), model.getGitRepository());
        if (StringUtils.isNotEmpty(model.getGitBranch())) {
            GitUtils.checkoutBranch(model.getContext(), model.getGitBranch());
        }
    }

    public static void mavenBuild(final GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        String[] mvnArgs = StringUtils.split(model.getMavenArgsWithVersion(), " ");
        List<String> effectiveProperties = getEffectivePropertiesForGitRepository(Arrays.asList(mvnArgs));
        MavenUtils.build(model.getContext(), model.getApplicationFolder(), effectiveProperties);
    }

    public static List<String> getEffectivePropertiesForGitRepository(List<String> properties) {
        List<String> effectiveProperties = new LinkedList<>(properties);
        effectiveProperties.add(MavenUtils.withProperty(QUARKUS_VERSION, QUARKUS_VERSION_VALUE));

        if (StringUtils.isEmpty(PLUGIN_VERSION.get())) {
            effectiveProperties.add(MavenUtils.withProperty(QUARKUS_PLUGIN_VERSION, QUARKUS_VERSION_VALUE));
        }

        return effectiveProperties;
    }
}
