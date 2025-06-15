#!/bin/bash
set -e

releases=${RELEASES_JSON:-$(curl -sSL "https://api.github.com/repos/${GITHUB_REPOSITORY}/releases?per_page=100")}
current_release=${LATEST_JSON:-$(curl -sSL "https://api.github.com/repos/${GITHUB_REPOSITORY}/releases/latest" | jq -r .tag_name)}

if [[ "$GITHUB_BASE_REF" == "main" ]]; then
  latest_prerelease=$(echo "$releases" | jq -r '.[] | select(.prerelease) | .tag_name' | head -n 1)
  latest_prerelease_beta_number=$(echo "$latest_prerelease" | cut -d"." -f4,4 | grep -o '[0-9]\+' || true)
  latest_prerelease_minor_version=$(echo "$latest_prerelease" | cut -d'.' -f2,2)

  next_prerelease=$(grep -E 'current-version:\s*' project.yml | awk '{print $2}')
  next_prerelease_minor_version=$(echo "$next_prerelease" | cut -d"." -f2,2)
  next_prerelease_patch_version=$(echo "$next_prerelease" | cut -d"." -f3,3)
  next_prerelease_beta_number=$(echo "$next_prerelease" | cut -d"." -f4,4 | grep -o '[0-9]\+' || true)
  prerelease_minor_version_bump=false

  current_release_minor_version=$(echo "$current_release" | cut -d"." -f2,2)

  # Determine minor version bump (e.g., 1.6.0.Beta21 → 1.7.0.Beta1)
  if [[ "$next_prerelease_beta_number" == 1 ]]; then
    # Ensure patch version is zero when starting a new minor development cycle
    if [[ "$next_prerelease_patch_version" != 0 ]]; then
      echo "Error: pre-release patch version must be 0 when starting new minor version"
      exit 1;
    fi

    if [[ "$next_prerelease_minor_version" -eq $(("$latest_prerelease_minor_version" + 1)) ]]; then
      echo "Starting new minor development cycle after unreleased prerelease"
      prerelease_minor_version_bump=true
    elif [[ "$next_prerelease_minor_version" -eq $(("$current_release_minor_version" + 1)) ]]; then
      echo "Starting new minor development cycle after released version"
      prerelease_minor_version_bump=true
    else
      echo "Error: cannot skip minor versions. Must be based on latest prerelease or release"
      exit 1;
    fi
  elif [[ "$next_prerelease_beta_number" == 0 ]]; then
    echo "Error: pre-release beta number cannot be 0, it must be 1 or higher"
    exit 1;
  else
    # Regular pre-release continuation
    if [[ "$next_prerelease_minor_version" != "$current_release_minor_version" && "$next_prerelease_minor_version" != $(("$current_release_minor_version" + 1)) ]]; then
      echo "Error: pre-release minor version must match current or be next"
      exit 1;
    fi
  fi


  if [[ "$next_prerelease_patch_version" != 0 || -z "$next_prerelease_beta_number" ]]; then
    echo "Error: new releases are not allowed from development branch, use .Beta\\D+ qualifier"
    exit 1;
  fi

  if (! "$prerelease_minor_version_bump") && [[ ("$next_prerelease_beta_number" != $(("$latest_prerelease_beta_number" + 1))) ]]; then
    echo "Error: pre-release version should go one by one as sequence"
    correct_version=$(echo "$latest_prerelease" | cut -d"." -f1,2,3)
    correct_prerelease_number=$(("$latest_prerelease_beta_number" + 1))
    echo "After" $latest_prerelease "should go "$correct_version".Beta"$correct_prerelease_number
    exit 1;
  fi

  project_current_base=$(echo "$next_prerelease" | grep -oP '^.*Beta')
  project_next_version=$(grep -E 'next-version:\s*' project.yml | awk '{print $2}')
  project_next_base=$(echo "$project_next_version" | grep -oP '^.*Beta')

  project_current_beta_number="$next_prerelease_beta_number"
  project_next_beta_number=$(echo "$project_next_version" | grep -oP 'Beta\K[0-9]+')

  if [[ "$project_current_base" != "$project_next_base" ]] || [[ $(("$project_current_beta_number" + 1)) != "$project_next_beta_number" ]]; then
    echo "Error: the next-version in project.yaml is not valid. Next pre-release Beta version must be one upper that current"
    exit 1;
  fi

else
  expected_release_version=$(echo "$GITHUB_BASE_REF" | sed 's/z/0/')

  first_release_tag_exists=$(echo "$releases" | jq -r '.[] | .tag_name' | grep "^$expected_release_version$" || true)
  next_release=$(grep -E 'current-version:\s*' project.yml | awk '{print $2}')

  if [ -z "$first_release_tag_exists" ]; then
    if [[ "$next_release" != "$expected_release_version" ]]; then
      echo "Error: wrong tag name for the first release in new branch"
      exit 1;
    else
      exit 0;
    fi
  fi

  branch_version=$(echo "$GITHUB_BASE_REF" | cut -d. -f1,2)
  latest_branch_tag_patch_version=$(echo "$releases" | jq -r --arg version "$branch_version" '
    .[]
    | select(.tag_name | contains($version))
    | .tag_name' | grep -v "Beta" | head -1 | cut -d. -f3)

  branch_minor_version=$(echo "$GITHUB_BASE_REF" | cut -d. -f2,2)
  next_release_minor_version=$(echo "$next_release" | cut -d"." -f2,2)
  next_release_patch_version=$(echo "$next_release" | cut -d"." -f3,3)
  beta_tag_exists=$(echo "$next_release" | cut -d"." -f4,4 || true)

  if [ -n "$beta_tag_exists" ]; then
    echo "Error: releases cannot consist any qualifier after version"
    exit 1;
  fi

  if [[ "$branch_minor_version" != "$next_release_minor_version" ]]; then
    echo "Error: minor versions cannot be changed"
    exit 1;
  fi

  if [[ $(("$latest_branch_tag_patch_version" + 1)) != "$next_release_patch_version" ]]; then
    echo "Error: release patch versions should be bumped one by one as sequence"
    exit 1;
  fi

  project_next_version=$(grep -E 'next-version:\s*' project.yml | awk '{print $2}')
  project_next_base=$(echo "$project_next_version" | cut -d"." -f1,2)

  project_current_patch_version="$next_release_patch_version"
  project_next_patch_version=$(echo "$project_next_version" | awk -F. '{print $3}')

  if [[ "$branch_version" != "$project_next_base" ]] || [[ $(("$project_current_patch_version" + 1)) != "$project_next_patch_version" ]]; then
    echo "Error: the next-version in project.yaml is not valid. Patch version of the next release must be one upper than the latest"
    exit 1;
  fi
fi

