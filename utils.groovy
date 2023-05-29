// Utility methods for Jenkins pipelines

/**
  * Returns true if branch has changes in the specified path with the target branch.
  * If invertMatch is true, returns true if branch has changes that do not match the specified path.
  */
boolean hasChangesIn(String module, boolean invertMatch = false) {
  sh "echo 'Comparing changes in ${module} with ${env.CHANGE_TARGET}..${env.BRANCH_NAME}'"
  grepArgs = invertMatch ? '-v' : ''
  mergeBase = sh(
    returnStdout: true, script: "git merge-base origin/${env.BRANCH_NAME} origin/${env.CHANGE_TARGET}").trim()
  return !env.CHANGE_TARGET ||
  sh(
    returnStatus: true,
    script: "git diff --name-only ${mergeBase}..origin/${env.BRANCH_NAME} | grep ${grepArgs} \"${module}\"") == 0
}

/**
  * Adds a remote reference to the specified branch.
  */
void addRemoteRef(String branchName) {
  withCredentials([gitUsernamePassword(credentialsId: 'github-rstudio-jenkins', gitToolName: 'Default')]) {
    sh "git config --add remote.origin.fetch +refs/heads/${branchName}:refs/remotes/origin/${branchName}"
    sh "git fetch --no-tags --force --progress ${GIT_URL} refs/heads/${branchName}:refs/remotes/origin/${branchName}"
  }
}

/**
  * Get Version.
  * Does not work on windows.
  */
def getVersion(boolean isHourly) {
  def buildType = ""
  if(isHourly) {
    buildType="--build-type=hourly"
  }
  def rstudioVersion = sh(
                          script: "docker/jenkins/rstudio-version.sh --patch=${params.RSTUDIO_VERSION_PATCH} ${buildType}",
                          returnStdout: true
                        ).trim()
  echo "RStudio build version: ${rstudioVersion}"

  // Split on [-+] first to avoid having to worry about splitting out .pro<n>
  def version = rstudioVersion.split('[-+]')

  // extract major / minor /patch version
  def majorComponents = version[0].split('\\.')
  rstudioVersionMajor = majorComponents[0]
  rstudioVersionMinor = majorComponents[1]
  rstudioVersionPatch = majorComponents[2]

  // Extract suffix
  if (version.length > 2) {
    rstudioVersionSuffix = '-' + version[1] + '+' + version[2]
  }
  else {
    rstudioVersionSuffix = '+' + version[1]
  }

  return [rstudioVersion, rstudioVersionMajor, rstudioVersionMinor, rstudioVersionPatch, rstudioVersionSuffix]
}

def jenkins_user_build_args() {
  def jenkins_uid = sh (script: 'id -u jenkins', returnStdout: true).trim()
  def jenkins_gid = sh (script: 'id -g jenkins', returnStdout: true).trim()
  return " --build-arg JENKINS_UID=${jenkins_uid} --build-arg JENKINS_GID=${jenkins_gid}"
}

/**
  * Get the branch flower name from the version/RELEASE file
  */
def getFlower() {
  return readFile(file: 'version/RELEASE').replaceAll(" ", "-").toLowerCase().trim()
}

/**
  * Upload the package specified by packageFile to the location of destinationPath in the rstudio-ide-build S3 bucket.
  * Sets the correct ACLs.
  */
def uploadPackageToS3(String packageFile, String destinationPath) {
  s3Upload acl: 'BucketOwnerFullControl', bucket: "rstudio-ide-build", file: "${packageFile}", path: "${destinationPath}"
}

/**
  * Upload javascript source maps to Sentry.
  * Does not work on windows.
  */
def sentryUploadSourceMaps() {
  def retryCount = 0
  def ret = 1
  while (retryCount < 5 && ret != 0) {
    ret = sh returnStatus: true, script: 'sentry-cli --auth-token ${SENTRY_API_KEY} releases --org rstudio --project ide-backend files ' + RSTUDIO_VERSION + ' upload-sourcemaps --ext js --ext symbolMap --rewrite .'
    echo "Return code: ${ret}"
    if (ret != 0 && retryCount < 5) {
      sleep time: 30, unit: 'SECONDS'
      retryCount = retryCount + 1
    }
  }
}

/** 
  * Upload debug symbols to sentry. Symbol type should be dsym or elf. 
  * Does not work on windows.
  */
def sentryUpload(String symbolType) {
  def retryCount = 0
  def ret = 1
  while (retryCount < 5 && ret != 0) {
    ret = sh returnStatus: true, script: 'sentry-cli --auth-token ${SENTRY_API_KEY} upload-dif --org rstudio --project ide-backend -t ' + symbolType + ' .'
    echo "Return code: ${ret}"
    if (ret != 0 && retryCount < 5) {
      sleep time: 30, unit: 'SECONDS'
      retryCount = retryCount + 1
    }
  }
}

/** 
  * Publish a build to the dailies site.
  * Does not work on windows.
  */
def publishToDailiesSite(String packageFile, String destinationPath, String urlPath = '') {
  def channel = ''
  if (!params.DAILY) {
    channel = ' --channel Hourly'
  }
  if (urlPath == '')
  {
    urlPath = destinationPath
  }

  sh '${WORKSPACE}/docker/jenkins/publish-build.sh --pat ${GITHUB_LOGIN_PSW} ' +
    channel +
    ' --version ' +
    RSTUDIO_VERSION +
    ' --build ' +
    destinationPath +
    ' --url https://s3.amazonaws.com/rstudio-ide-build/' +
    urlPath +
    '/' +
    packageFile +
    ' --file ' +
    packageFile 
}

/** 
  * Convert an architecture to the operating specific version of that arch.
  * amd64  -> x86_64 on non-Debian
  * x86_64 -> amd64  on Debian
  */
def getArchForOs(String os, String arch) {
  if ((arch == "amd64") && (os != "focal") && (os != "jammy")) {
    return "x86_64"
  }

  if ((arch == "x86_64") && ((os == "focal") || (os == "jammy"))) {
    return "amd64"
  }

  if (arch == "aarch64") {
    return "arm64"
  }

  return arch
}

/**
  * Gets environment variasbles needed for running the build on Linux and Mac.
  * Does not work on windows.
  */
def getBuildEnv(boolean isHourly) {
  def env = "RSTUDIO_VERSION_MAJOR=${RSTUDIO_VERSION_MAJOR} RSTUDIO_VERSION_MINOR=${RSTUDIO_VERSION_MINOR} RSTUDIO_VERSION_PATCH=${RSTUDIO_VERSION_PATCH} RSTUDIO_VERSION_SUFFIX=${RSTUDIO_VERSION_SUFFIX}"
  if (isHourly) {
    env = "${env} SCCACHE_ENABLED=1"
  }

  return env
}

/** 
  * Get the name of the product based on the type of build
  */
def getProductName() {
  def name = FLAVOR.toLowerCase()
  if (IS_PRO && name != "server") {
    name = name + "-pro"
  } else if (IS_PRO) {
    name = "workbench"
  }

  return name
}

/**
  * Upload dailiy redirects.
  * Does not work on windows.
  */
def updateDailyRedirects(String path) {
  sh 'docker/jenkins/publish-daily-binary.sh https://s3.amazonaws.com/rstudio-ide-build/' + path + ' ${RSTUDIO_ORG_PEM}'
}

/**
  * This method exists to quickly reenable our builds with 
  * a bionic docker image, however our builds still need to
  * be labeled focal. This is to support Debian 10 and
  * should be retired once Debian 10 falls out of support
  */
def getDockerBuildOs(String osName) {
  if(osName == "focal"){
    return "bionic"
  } else {
    return osName
  }
}

/**
  * Don't try to change RSTUDIO_VERSION_FLOWER to env.RSTUDIO_VERSION_FLOWER
  * in order for it to match, because for some reason that causes it to
  * resolve to "null". I don't know why.
  */
def getDockerTag() {
  return "${env.IS_PRO ? 'pro-' : ''}${getDockerBuildOs(env.OS)}-${env.ARCH}-${RSTUDIO_VERSION_FLOWER}"
}

return this
