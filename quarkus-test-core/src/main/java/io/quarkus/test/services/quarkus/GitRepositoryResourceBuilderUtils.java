package io.quarkus.test.services.quarkus;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.utils.GitUtils;

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

}
