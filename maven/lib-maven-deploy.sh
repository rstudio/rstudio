# Locate Maven exe
export MAVEN_BIN=${MAVEN_BIN:=`which mvn`}
if [ -z "$MAVEN_BIN" ]; then
  echo "mvn not found. Add mvn to PATH or set MAVEN_BIN."
  exit
fi
echo Using $MAVEN_BIN

function set-random-dir() {
  export RANDOM_DIR=/tmp/random-dir-$RANDOM$RANDOM$RANDOM$RANDOM
  rm -rf $RANDOM_DIR
  mkdir -p $RANDOM_DIR
}

# arguments:
#   * url to maven repository (required)
#   * id of maven repository, to be used to get credentials from settings.xml
#     (required)
#   * artifact to upload (required)
#   * pom file for this artifact, if the artifact is the pom, this must be the same
#     value (required)
#   * javadoc artifact to upload (optional)
#   * sources artifact to upload (optional, but requires a javadoc artifact)
function maven-deploy-file() {
  local mavenRepoUrl=$1
  shift
  local mavenRepoId=$1
  shift
  local curFile=$1
  shift
  local pomFile=$1
  shift

  if [ $# -ne 0 ] && [ "$1" != "" ]; then
    local javadoc="-Djavadoc=$1"
    shift
  fi
  if [ $# -ne 0 ] && [ "$1" != "" ]; then
    local sources="-Dsources=$1"
    shift
  fi

  if [[ "$curFile" == "" ]]; then
    echo "ERROR: Unable to deploy $artifactId in repo! Cannot find corresponding file!"
    return 1
  fi

  local cmd="";
  if [[ "$gpgPassphrase" != "" ]]; then
    cmd="$MAVEN_BIN \
           org.apache.maven.plugins:maven-gpg-plugin:1.4:sign-and-deploy-file \
            -Dfile=$curFile \
            -Durl=$mavenRepoUrl \
            -DrepositoryId=$mavenRepoId \
            -DpomFile=$pomFile \
            -DuniqueVersion=false \
            $javadoc \
            $sources \
            -Dgpg.passphrase=\"$gpgPassphrase\""
  else
    echo "GPG passphrase not specified; will attempt to deploy files without signing"
    cmd="$MAVEN_BIN \
           org.apache.maven.plugins:maven-deploy-plugin:2.7:deploy-file \
            -Dfile=$curFile \
            -Durl=$mavenRepoUrl \
            -DrepositoryId=$mavenRepoId \
            -DpomFile=$pomFile \
            $javadoc \
            $sources \
            -DuniqueVersion=false"
  fi
  eval $cmd
}

