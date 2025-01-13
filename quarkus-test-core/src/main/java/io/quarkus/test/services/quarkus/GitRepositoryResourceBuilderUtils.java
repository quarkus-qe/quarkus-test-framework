package io.quarkus.test.services.quarkus;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.utils.GitUtils;
import io.quarkus.test.utils.MavenUtils;

public final class GitRepositoryResourceBuilderUtils {

    private GitRepositoryResourceBuilderUtils() {

    }

    public static void cloneRepository(final GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        GitUtils.cloneRepository(model.getContext(), model.getGitRepository());
        if (StringUtils.isNotEmpty(model.getGitBranch())) {
            GitUtils.checkoutBranch(model.getContext(), model.getGitBranch());
        }
        GitUtils.showRepositoryState(model.getContext());
    }

    public static void mavenBuild(final GitRepositoryQuarkusApplicationManagedResourceBuilder model) {
        String[] mvnArgs = StringUtils.split(model.getMavenArgsWithVersion(), " ");
        MavenUtils.build(model.getContext(), model.getApplicationFolder(), Arrays.asList(mvnArgs));
    }

}
