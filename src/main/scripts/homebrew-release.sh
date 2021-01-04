set -e

readonly JCLIFF_TAG="${{ steps.get_version.outputs.TAG_VERSION }}"
readonly HOMEBREW_TAP_REPO="${HOMEBREW_TAP_REPO:-redhat-cop/homebrew-redhat-cop}"
readonly REPO_NAME=$(basename $HOMEBREW_TAP_REPO)

readonly SHA256=$(sha256sum target/jcliff-${JCLIFF_TAG:1}-dist.tar.gz | cut -f1 -d ' ')
readonly URL="https://github.com/bserdar/jcliff/releases/download/${JCLIFF_TAG}/jcliff-${JCLIFF_TAG:1}-dist.tar.gz"

rm -rf "${REPO_NAME}"

git clone https://github.com/${HOMEBREW_TAP_REPO}.git

sed -i -e 's|^  url.*|  url "'"$URL"'"|g' ${REPO_NAME}/Formula/jcliff.rb
sed -i -e 's|^  sha256.*|  sha256 "'"$SHA256"'"|g' ${REPO_NAME}/Formula/jcliff.rb

cd $(basename "$HOMEBREW_TAP_REPO")

git config user.name "${BREW_GITHUB_USER:-Andrew Block}"
git config user.email "${BREW_GITHUB_EMAIL:-andy.block@gmail.com}"

git add Formula/jcliff.rb
git commit -m "jcliff ${JCLIFF_TAG} release"
git tag -a "jcliff-${JCLIFF_TAG}" -m "jcliff ${JCLIFF_TAG}"

if [[ ! -z $BREW_GITHUB_USERNAME && ! -z $BREW_GITHUB_TOKEN ]]; then
git push https://${BREW_GITHUB_USERNAME}:${BREW_GITHUB_TOKEN}@github.com/${HOMEBREW_TAP_REPO}.git --follow-tags
else
echo "BREW_GITHUB_USERNAME and BREW_GITHUB_TOKEN variables not set. Skipping push..."
fi