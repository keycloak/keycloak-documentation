#!/bin/bash

# Add a comment to a GitHub issue/PR

set -eEu

gh_ref="${1?Specify GitHub repository. ex: keycloak/keycloak-documentation}"
issue_num="${2?Specify issue/PR number}"
shift 2
comment_body="${*?Specify comment body text}"
gh_token="${GITHUB_TOKEN?Specify a GitHub Personal Access Token with public_repo scope}"

[[ "$issue_num" == "false" ]] && exit 0

die() {
    echo "$@" >&2
    exit 1
}

curl -sS -X POST -H "Content-Type: application/json" -H "Authorization: token $gh_token" --data '{ "body": "'"${comment_body//\"/\\\"}"'" }' "https://api.github.com/repos/$gh_ref/issues/$issue_num/comments"
