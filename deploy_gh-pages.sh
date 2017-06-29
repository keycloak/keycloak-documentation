#!/bin/bash

# A customized dpl/lib/dpl/provider/pages.rb
# Performs an additive deploy to GitHub Pages

set -eEu

gh_ref="github.com/${1?Specify GitHub repository. ex: keycloak/keycloak-documentation}.git"
local_dir="${2?Specify local directory to deploy from. ex: aggregation/}"
gh_token="${GITHUB_TOKEN?Specify a GitHub Personal Access Token with public_repo scope}"
target_branch="${DEPLOY_TARGET_BRANCH:-gh-pages}"
gh_email="${DEPLOY_GH_EMAIL:-deploy@travis-ci.org}"
gh_name="${DEPLOY_GH_NAME:-Deployment Bot (from Travis CI)}"

die() {
    echo "$@" >&2
    exit 1
}

[ ! -e "$local_dir/.git" ] || die "Local directory $local_dir is a git repository"

# Create temporary directory
td="$(mktemp -d)"
trap "rm -rf '$td'" EXIT

git init "$td"

local_dir="$(readlink -e "$local_dir")"
cd "$td"

# Must ensure the clone URL is not printed, it contains the token!
{ set +x; } 2>/dev/null
echo "${PS4}git remote add origin 'https://...@$gh_ref'" >&2
git remote add origin "https://$gh_token@$gh_ref" >/dev/null 2>&1
set -x

git config --local user.email "$gh_email"
git config --local user.name "$gh_name"

retry() {
    set -eE

    # Must ensure the remote URL is not printed, it contains the token!
    git fetch --depth 1 origin "$target_branch" >/dev/null 2>&1

    git checkout -b "$(date +%s.%N)" "origin/$target_branch"
    cp -a "$local_dir/." "."
    git add -A
    git commit -m "Deploy"

    # Must ensure the remote URL is not printed, it contains the token!
    if ! git push origin "HEAD:$target_branch" >/dev/null 2>&1
    then
        git reset --hard "origin/$target_branch"
        false
    fi
}

# Must ensure the remote URL is not printed, it contains the token!
retry || { sleep 2; retry; } || { sleep 4; retry; } || echo "Giving up on deploy"
