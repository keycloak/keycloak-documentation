#!/bin/bash

set -eu

# When handling a pull request, end the build early when changes were made to restricted files

exclude_pattern="${1?Specify an extended-regex exclusion pattern, ex: '\.(xml|sh)$'}"
# Example output from git diff --name-only:
# .travis.yml
# OVERVIEW.adoc
# server_installation/topics/operating-mode.adoc
# server_installation/topics/overview.adoc
# server_installation/topics/overview/recommended-reading.adoc

if [ -n "$TRAVIS_PULL_REQUEST_BRANCH" ]
then
    if git diff --name-only -z "$TRAVIS_COMMIT_RANGE" -- | grep -ZEqe "$exclude_pattern"
    then
        return 2
    fi
    [ $PIPESTATUS -eq 0 ] || travis_terminate 3
fi
