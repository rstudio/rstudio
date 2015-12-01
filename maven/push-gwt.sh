#!/bin/bash
#
# Pushes GWT artifacts to a local (the default) or remote maven repository
# To push remote, set 2 env variables: GWT_MAVEN_REPO_URL and GWT_MAVEN_REPO_ID
#
# GWT_MAVEN_REPO_ID = a server id in your .m2/settings.xml with remote repo username and password
#
# Sonatype staging repo (promotes to Maven Central)
#   GWT_MAVEN_REPO_URL=https://oss.sonatype.org/service/local/staging/deploy/maven2/
#
# Sonatype Google SNAPSHOTs repo (can only deploy SNAPSHOTs here, and they are immediately public)
#   GWT_MAVEN_REPO_URL=https://oss.sonatype.org/content/repositories/google-snapshots/

pushd $(dirname $0) >/dev/null 2>&1

export pomDir=./poms

source lib-gwt.sh

# use GWT_MAVEN_REPO_URL if set else M2_REPO else default location for local repo
localRepoUrl=${M2_REPO:="$HOME/.m2/repository"}
localRepoUrl="file://$localRepoUrl"
repoUrlDefault=${GWT_MAVEN_REPO_URL:=$localRepoUrl}
# repo id is ignored by local repo
repoId=${GWT_MAVEN_REPO_ID:=none}

# use GWT_DIST_FILE to specify the default distribution file
gwtTrunk=$(dirname $(pwd))
gwtPathDefault=${GWT_DIST_FILE:=$(ls -t1 ${gwtTrunk}/build/dist/gwt-*.zip 2>/dev/null | head -1)}
if [[ -f "$gwtPathDefault" ]]; then
  gwtPathPrompt="($gwtPathDefault)"
fi

VERSION_REGEX='[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*-*.*'

# use GWT_VERSION to specify the default version or get it from the file name
gwtVersionDefault=${GWT_VERSION:=$(expr "$gwtPathDefault" : '.*gwt-\('$VERSION_REGEX'\)\.zip')}
jsinteropVersionDefault=${JSINTEROP_VERSION:=1.0.0-SNAPSHOT}

# prompt for info
read -e -p"GWT version for Maven (${gwtVersionDefault:-ex: 2.8.0-SNAPSHOT}): " gwtVersion
gwtVersion=${gwtVersion:=$gwtVersionDefault}
if ! expr "$gwtVersion" : "$VERSION_REGEX" >/dev/null; then
  echo "Please enter a version of the form x.y.z or x.y.z-abc"
  exit 1
fi

read -e -p"JsInterop version for Maven (${jsinteropVersionDefault:-ex: 1.0.0-SNAPSHOT}): " jsinteropVersion
jsinteropVersion=${jsinteropVersion:=$jsinteropVersionDefault}
if ! expr "$jsinteropVersion" : "$VERSION_REGEX" >/dev/null; then
  echo "Please enter a version of the form x.y.z or x.y.z-abc"
  exit 1
fi

read -e -p"Path to GWT distro zip $gwtPathPrompt: " gwtPath
gwtPath=${gwtPath:=$gwtPathDefault}
if [[ ! -f  $gwtPath ]]; then
  echo "ERROR: Cannot find file at \"$gwtPath\""
  exit 1
fi

read -e -p"Deploy to repo URL ($repoUrlDefault): " repoUrl
repoUrl=${repoUrl:=$repoUrlDefault}

# setting the repoUrl to 'install' will instruct to maven-gwt to
# execute the install goal instead of the deploy one.
if [[ "$repoUrl" == "$localRepoUrl" ]]; then
  repoUrl=install
fi

# use GWT_GPG_PASS environment var by default if set
read -p"GPG passphrase for jar signing (may skip for local deployment): " gpgPassphrase
gpgPassphrase=${gpgPassphrase:=$GWT_GPG_PASS}

maven-gwt "$gwtVersion" \
          "$jsinteropVersion" \
          "$gwtPath" \
          "$repoUrl" \
          "$repoId"

popd >/dev/null 2>&1
