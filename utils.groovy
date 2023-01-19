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
def getVersion(boolean isHourly = false) {
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

return this
