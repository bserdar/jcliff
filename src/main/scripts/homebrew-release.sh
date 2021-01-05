#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

readonly HOMEBREW_TAP_REPO_HOST=${HOMEBREW_TAP_REPO_HOST:-'github.com'}
readonly HOMEBREW_TAP_REPO="${HOMEBREW_TAP_REPO:-ansible-middleware/homebrew-tap}"
readonly REPO_NAME=$(basename $HOMEBREW_TAP_REPO)
readonly GITHUB_RELEASES_URL=${GITHUB_RELEASES_URL:-'https://github.com/bserdar/jcliff/releases/download'}
readonly DIST_TARBALL_URL=${DIST_TARBALL_URL:-"${GITHUB_RELEASES_URL}/${JCLIFF_TAG}/jcliff-${JCLIFF_TAG:1}-dist.tar.gz"}
readonly FORMULA_PATH="${FORMULA_PATH:-'Formula/jcliff.rb'}"

readonly SHA256=$(sha256sum "target/jcliff-${JCLIFF_TAG:1}-dist.tar.gz" | cut -f1 -d ' ')

rm -rf "${REPO_NAME}"

git clone "https://${HOMEBREW_TAP_REPO_HOST}/${HOMEBREW_TAP_REPO}.git"

sed -i -e 's|^  url.*|  url "'"$DIST_TARBALL_URL"'"|g' "${REPO_NAME}/${FORMULA_PATH}"
sed -i -e 's|^  sha256.*|  sha256 "'"$SHA256"'"|g' "${REPO_NAME}/${FORMULA_PATH}"

cd $(basename "$HOMEBREW_TAP_REPO")

git config user.name "${BREW_GITHUB_USER:-'Andrew Block'}"
git config user.email "${BREW_GITHUB_EMAIL:-'andy.block@gmail.com'}"

git add "${FORMULA_PATH}"
git commit -m "jcliff ${JCLIFF_TAG} release"
git tag -a "jcliff-${JCLIFF_TAG}" -m "jcliff ${JCLIFF_TAG}"

if [[ ! -z "${BREW_GITHUB_USERNAME}" && ! -z "${BREW_GITHUB_TOKEN}" ]]; then
git push "https://${BREW_GITHUB_USERNAME}:${BREW_GITHUB_TOKEN}@${HOMEBREW_TAP_REPO_HOST}/${HOMEBREW_TAP_REPO}.git" --follow-tags
else
echo "BREW_GITHUB_USERNAME and BREW_GITHUB_TOKEN variables not set. Skipping push..."
fi