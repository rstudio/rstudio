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
    script: "git diff --name-only --quiet ${mergeBase}..origin/${env.BRANCH_NAME} | grep ${grepArgs} \"${module}\"") == 1
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
  * Get Version
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

def publishToDailiesSite(String packageFile, String destinationPath) {
  sh '${WORKSPACE}/docker/jenkins/publish-build.sh --pat ${GITHUB_LOGIN_PSW} --version ' +
    RSTUDIO_VERSION +
    ' --build ' +
    destinationPath +
    ' --url https://s3.amazonaws.com/rstudio-ide-build/' +
    destinationPath +
    '/' +
    packageFile +
    ' --file ' +
    packageFile
}

def getArchForOs(String os, String arch) {
  if ((arch == "amd64") && (os != "bionic") && (os != "jammy")) {
    return "x86_64"
  }

  if (arch == "aarch64") {
    return "arm64"
  }

  return arch
}

def getBuildEnv() {
  def env = "RSTUDIO_VERSION_MAJOR=${RSTUDIO_VERSION_MAJOR} RSTUDIO_VERSION_MINOR=${RSTUDIO_VERSION_MINOR} RSTUDIO_VERSION_PATCH=${RSTUDIO_VERSION_PATCH} RSTUDIO_VERSION_SUFFIX=${RSTUDIO_VERSION_SUFFIX}"
  if (DAILY == false) {
    env = "${env} SCCACHE_ENABLED=1"
  }

  return env
}

def getProductName() {
  def name = FLAVOR.toLowerCase()
  if (IS_PRO && name != "server") {
    name = name + "-pro"
  } else if (IS_PRO) {
    name = "workbench"
  }

  return name
}

def uploadDailyRedirects(String path) {
  sh 'docker/jenkins/publish-daily-binary.sh https://s3.amazonaws.com/rstudio-ide-build/' + path + ' ${RSTUDIO_ORG_PEM}'
}

return this
