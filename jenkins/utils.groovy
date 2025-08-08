// Utility methods for Jenkins pipelines

/**
  * Returns true if branch has changes in the specified path with the target branch.
  * If invertMatch is true, returns true if branch has changes that do not match the specified path.
  */
boolean hasChangesIn(String module, boolean invertMatch = false, boolean useRegex = false) {
  sh "echo 'Comparing changes in ${module} with ${env.CHANGE_TARGET}..${env.BRANCH_NAME}'"
  grepArgs = invertMatch ? 'v' : ''
  grepArgs = useRegex ? "P${grepArgs}" : grepArgs
  grepArgs = grepArgs.isEmpty() ? '' : "-${grepArgs}"
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
  withCredentials([gitUsernamePassword(credentialsId: 'posit-jenkins-rstudio', gitToolName: 'Default')]) {
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
                          script: "docker/jenkins/rstudio-version.sh ${buildType}",
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

def getBaseCommit() {
  return sh(
            script: "docker/jenkins/rstudio-base-commit.sh",
            returnStdout: true
          ).trim()
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

def getVersionFilename(String version) {
  return version.replaceAll('[^a-zA-Z0-9-]', '-').replaceAll('--*', '-').trim()
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
def publishToDailiesSite(String packageFile, String destinationPath, String urlPath = '', String arch = '') {
  def channel = ''
  if (!params.DAILY) {
    channel = ' --channel Hourly'
  }
  if (urlPath == '')
  {
    urlPath = destinationPath
  }
  archArg = ''
  if (arch != '') {
    archArg = ' --arch ' + arch
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
    packageFile +
    archArg
}

/**
 * Sometimes a specific OS/ARCH/FLAVOR needs to do an additional publish.
 * Handle that here.
 */
def optionalPublishToDailies(String packageFile, String destinationPath, String urlPath = '', String product = '') {
  // Define OS mappings for republishing compatible binaries
  def osRepublishMappings = [
    'jammy': 'noble',  // Noble and Jammy use the same binaries
    'rhel9': 'rhel10'  // RHEL 10 and RHEL 9 use the same binaries
  ]
  
  def currentOs = env.OS
  if (osRepublishMappings.containsKey(currentOs)) {
    republishForCompatibleOs(packageFile, destinationPath, urlPath, product, osRepublishMappings[currentOs])
  }
}

/**
 * Helper function to republish a build for a compatible OS
 */
def republishForCompatibleOs(String packageFile, String destinationPath, String urlPath, String product, String targetOs) {
  if (product == '') {
    // If the product name is not passed as a parameter, we expect it
    // to be set in the environment.
    product = "${env.PRODUCT}"
  }
  
  def targetDailiesPath = "${product}/${targetOs}-${getArchForOs(targetOs, env.ARCH)}"
  if (destinationPath.contains("-xcopy")) {
    targetDailiesPath = "${targetDailiesPath}-xcopy"
  }
  
  publishToDailiesSite(packageFile, targetDailiesPath, urlPath)
}

/**
 * Return true if the file at the URL exists. Return false otherwise
 */
def urlExists(String url) {
  return sh( returnStatus: true, script: 'curl --head --silent --fail ' + url + ' 2> /dev/null') == 0
}

/**
 * Convert an architecture to the operating specific version of that arch.
 * amd64  -> x86_64 on non-Debian
 * x86_64 -> amd64  on Debian
 * noarch -> all    on Debian
 */
def getArchForOs(String os, String arch) {
  def debianLikeOS = ["focal", "jammy", "noble"]
  def isDebianLike = debianLikeOS.contains(os)

  if ((arch == "noarch") && isDebianLike) {
    return "all"
  }

  if ((arch == "amd64") && !isDebianLike) {
    return "x86_64"
  }

  if ((arch == "x86_64") && isDebianLike) {
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
  * Gets the Linux agent label based on the arch.
  */
def getLinuxAgentLabel(String arch) {
  if (arch == 'arm64') {
    return 'linux && arm64 && 4x'
  }

  return 'linux-4x && x86_64'
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
  * Don't try to change RSTUDIO_VERSION_FLOWER to env.RSTUDIO_VERSION_FLOWER
  * in order for it to match, because for some reason that causes it to
  * resolve to "null". I don't know why.
  */
def getDockerTag() {
  return "${IS_PRO ? 'pro-' : ''}${env.OS}-${env.ARCH}-${RSTUDIO_VERSION_FLOWER}"
}

/**
  * Resolve the wildcards to find a file on the filesystem
  */
def resolveFilename(String dir, String match) {
  def file = sh(script: "basename `ls ${dir}/${match}`", returnStdout: true).trim()
  return file
}

/**
  * Rename the file on the filesystem
  */
def renameFile(String dir, String match) {
  def file = sh(script: "basename `ls ${dir}/${match}`", returnStdout: true).trim()
  def renamedFile = file.replace('-relwithdebinfo', '')
  sh "mv ${dir}/${file} ${dir}/${renamedFile}"
  return renamedFile
}

/**
  * Rename the file on the filesystem
  */
def renameTarFile(String dir) {
  if (FLAVOR == "Electron") {
    return renameFile(dir, "*.tar.gz")
  }

  return ""
}

def getAgentLabel(String arch) {
  if (arch == 'arm64') {
    return 'linux && arm64 && 4x'
  }

  return 'linux-4x && x86_64'
}

def isFlavorAProServerProduct() {
  serverProducts = ["Server", "selinux", "session", "monitor"]
  return serverProducts.contains(env.FLAVOR.toString())
}

// Use a function to keep the pipeline length down.
def shouldBuild(boolean isDaily, boolean isPro) {
  echo "Checking if we should build ${env.FLAVOR} on ${env.OS}-${env.ARCH}"
  def matchesFilter = ((params.OS_FILTER == env.OS ||  params.OS_FILTER == 'all') &&
    (params.ARCH_FILTER == env.ARCH ||  params.ARCH_FILTER == 'all') &&
    (params.FLAVOR_FILTER == env.FLAVOR ||  params.FLAVOR_FILTER == 'all'))

  // Filter hourlies based on https://github.com/rstudio/rstudio-pro/issues/4143#issuecomment-1362142399
  def inHourlySubset = true
  if (!isDaily) {
    if (isPro) {
      // build a set of x86_64 rhel9 + ubuntu22/24 server products and an arm64 jammy Electron RDP
      inHourlySubset = ((env.OS == 'rhel9' && env.ARCH == 'x86_64' && isFlavorAProServerProduct()) ||
        (env.OS == 'jammy' && env.ARCH == 'x86_64' && isFlavorAProServerProduct()) ||
        (env.OS == 'jammy' && env.ARCH == 'arm64' && env.FLAVOR == 'Electron'))
    } else {
      // build an arm64 rhel9 Electron and an x86_64 jammy Server
      inHourlySubset = ((env.OS == 'rhel9' && env.ARCH == 'arm64' && env.FLAVOR == 'Electron') ||
        (env.OS == 'jammy' && env.ARCH == 'x86_64' && env.FLAVOR == 'Server'))
    }
  }

  return matchesFilter && inHourlySubset
}

/**
  * Check if this specific arch/product/os build exists on the dailies
  * page, or if FORCE_BUILD_BINARIES is set.
  * Return true if we should build, false otherwise.
  */
def rebuildCheck() {
  if (params.FORCE_BUILD_BINARIES) {
    echo "Building ${env.ARCH} ${env.PRODUCT} on ${env.OS} because FORCE_BUILD_BINARIES is set"
    return true
  }
  else {
    def buildType = ""
    if (RSTUDIO_VERSION_FILENAME.contains("hourly")) {
      buildType = "rstudio-hourly"
    }
    else {
      buildType = "rstudio"
    }

    def osArchName = ""
    if (OS == "macos" || OS == "windows") {
      osArchName = OS
    }
    else {
      osArchName = "${env.OS}-${getArchForOs(env.OS, env.ARCH)}"
    }

    def retVal = !urlExists("https://dailies.rstudio.com/${buildType}/${RSTUDIO_VERSION_FLOWER}/${getProductName()}/${osArchName}/${RSTUDIO_VERSION_FILENAME}/index.html")

    if (!retVal) {
      echo "Skipping build of ${env.ARCH} ${env.PRODUCT} on ${env.OS} version ${RSTUDIO_VERSION_FILENAME} because it already exists on dailies site."
    }

    return retVal
  }
}

def prApiUrl() {
  ownerAndRepo = env.GIT_URL.replaceAll('^https://github.com[/:]', '').replaceAll('.git$', '')
  return "https://api.github.com/repos/${ownerAndRepo}/check-runs"
}

def checkRunsRequestWithRetry(String method, String payload = "", String url = prApiUrl(), int maxRetries = 3) {
  def lastResponse = null
  
  for (int attempt = 1; attempt <= maxRetries; attempt++) {
    echo "Attempt ${attempt} of ${maxRetries} - using fresh credentials"
    
    withCredentials([usernamePassword(credentialsId: 'posit-jenkins-rstudio', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
      if (method == "GET") {
        lastResponse = httpRequest(
          url: url,
          httpMode: method,
          customHeaders: [
            [name: "Authorization", value: 'token ' + GITHUB_TOKEN, maskValue: true],
            [name: 'X-GitHub-Api-Version', value: '2022-11-28'],
            [name: 'Accept', value: 'application/vnd.github+json'],
          ],
          validResponseCodes: "100:599"
        )
      } else {
        lastResponse = httpRequest(
          url: url,
          httpMode: method,
          requestBody: payload,
          customHeaders: [
            [name: "Authorization", value: 'token ' + GITHUB_TOKEN, maskValue: true],
            [name: 'X-GitHub-Api-Version', value: '2022-11-28'],
            [name: 'Accept', value: 'application/vnd.github+json'],
          ],
          validResponseCodes: "100:599"
        )
      }
    }
    
    if (lastResponse.status != 401) {
      return lastResponse
    }
    
    if (attempt < maxRetries) {
      echo "Received 401 response. Waiting before retry..."
      sleep time: Math.pow(2, attempt), unit: 'SECONDS' // Exponential backoff
    }
  }
  
  echo "All ${maxRetries} attempts failed with 401. Returning last response."
  return lastResponse
}

checks = [:]

/**
  * Posts a review check to the GitHub /check-runs API with the specified args.
  *
  * If the check does not exist, it will be created with default values.
  * If the check has already been posted in the current pipeline build, existing values will be used.
  *
  * Map args:
  *    title:   The title of the check (required)
  *    status:  The status of the check, one of 'queued', 'in_progress',
  *             'action_required', 'cancelled', 'failure', 'neutral', 'success',
  *             'skipped', 'timed_out' (optional, default: 'queued')
  *    summary: The summary of the check (optional, default: '')
  *    details: The details of the check (optional, default: '')
 */
synchronized boolean postReviewCheck(Map args) {
  if (!env.GIT_BRANCH) { return false }
  if (!env.GIT_BRANCH.startsWith('PR-')) { return false }

  def title
  if (args.title) {
    title = args.title
  } else {
    return false
  }

  def check = checks.get(title)
  if (!check) {
    def name = title.toLowerCase().replaceAll(' ', '-')
    check = [
      title: title,
      name: name,
      status: 'queued',
      summary: '',
      details: '',
    ]
    checks.put(title, check)
  }

  if (args.status) {
    check.status = args.status
  }
  if (args.summary) {
    check.summary = args.summary
  }
  if (args.details) {
    check.details = args.details
  }

  def text = check.details.trim()
    .replaceAll('^"', '')
    .replaceAll('"$', '')
    .replaceAll("'", "\\'")
    .replaceAll("\"", "\\\"")
    .replaceAll(/\R/, "\\\n")
    .replaceAll(/\\n/, "\\\n")
    
  // If text has more than 65000 characters, truncate it to 65000
  // GitHub API has a limit of 65536 characters for the text field
  if (text.length() > 65000) {
    text = text.substring(0, 65000)
    text += "\n\n[truncated]. See the full output in the Jenkins build log."
  }

  if (text) {
    text = "```bash\n${text}\n```"
  }
  def output = [
    "title": check.title,
    "summary": check.summary,
    "text": "${text}",
  ]
  def statusField = 'status'
  switch(check.status) {
    case 'queued':
    case 'in_progress':
      statusField = 'status'
      break
    case 'action_required':
    case 'cancelled':
    case 'failure':
    case 'neutral':
    case 'success':
    case 'skipped':
    case 'timed_out':
      statusField = 'conclusion'
      break
  }
  def payloadDict = [
    "${statusField}": check.status,
    "output": output,
    "name": check.name,
    "head_sha": GIT_COMMIT
  ]
  def payload = writeJSON(json: payloadDict, returnText: true)

  // archive the file
  def response = checkRunsRequestWithRetry('POST', payload)

  if (response.status == 201) {
    checks[title] = check
    responseContent = readJSON(text: response.content)
    checks[title].id = responseContent.id
    return true
  } else {
    echo "Failed to post review check: ${response.status} ${response.content}"
    return false
  }
}

def finishReviewChecks(String buildResult) {
  for (check in checks.values()) {
    if (buildResult == 'ABORTED') {
      postReviewCheck([
        title: check.title,
        status: 'cancelled',
        summary: 'Build was aborted',
      ])
    }
    else {
      response = checkRunsRequestWithRetry('GET', "", "${prApiUrl()}/${check.id}")

      if (response.status != 200) {
        echo "Failed to get check status: ${response.status} ${response.content}"
        continue
      }

      def checkResponse = readJSON(text: response.content)
      check.status = checkResponse.status

      if (check.status == 'in_progress') {
        postReviewCheck([
          title: check.title,
          status: 'failure',
        ])
      }
      else if (check.status == 'queued') {
        postReviewCheck([
          title: check.title,
          status: 'skipped',
        ])
      }
    }
  }
}

def runCmd(String cmd) {

  def file = "${UUID.randomUUID()}"
  def status = sh(
    script: """#!/usr/bin/env bash
set -o pipefail
${cmd} 2>&1 | tee ${file}
""",
    returnStatus: true
  )
  def output = fileExists(file) ? readFile(file: file) : '<no output available>'
  return [status, output]

}

def getResultsMarkdownLink(String name, String url) {
  return "[View ${name} results in Jenkins :link:](${url})"
}

def runCheckCmd(String cmd, String checkName, String stageUrl, boolean hideDetails = false) {

  postReviewCheck([
    title: checkName,
    status: 'in_progress',
    summary: getResultsMarkdownLink(checkName, stageUrl),
  ])

  (exitCode, out) = runCmd(cmd)
  success = exitCode == 0

  status = success ? "success" : "failure"
  def text = hideDetails ? '<details hidden>' : out

  postReviewCheck([
    title: checkName,
    status: status,
    details: text,
  ])

  if (!success) {
    sh "exit 1"
  }

}


def getStageUrl(String stageDisplayName) {
    buildUrl = env.BUILD_URL

    nodeRequestUrl = "${buildUrl}api/json?tree="+ URLEncoder.encode("actions[parameters[name,value],nodes[displayName,id]]", "UTF-8")

    jsonText = sh(
      returnStdout: true,
      script: 'curl -u $JENKINS_CREDENTIALS -H "Content-Type: application/json" -H "Accept: application/json"' + " ${nodeRequestUrl}"
    )
    json = readJSON(text: jsonText)

    nodes = json.actions.find { it._class == 'org.jenkinsci.plugins.workflow.job.views.FlowGraphAction' }?.nodes

    if (nodes) {
      // If there is a branch node, use that to find construct a valid URL
      nodeId = nodes.find { it.displayName == "Branch: ${stageDisplayName}" }?.id
      if (!nodeId) {
        nodeId = nodes.find { it.displayName == stageDisplayName }?.id
      }
    }
    if (!nodeId) {
      // if we didn't couldn't find the right node, just link to the build
      return buildUrl
    }
    return "${buildUrl}pipeline-console/?selected-node=${nodeId}"
}

def ninjaCmd() {
  return "ninja"
}

/**
 * Get a specific document property for a given document "flavor" from the build matrix
 * @param flavor The document flavor identifier
 * @param property The property name to retrieve ('buildDir', 'uploadSrc', 'uploadDst', 'revision')
 * @return The property value for the specified flavor or null if not found
 *
 * If the value of the doc matrix FLAVOR axis changes, these values *MUST* be updated to match
 */
def getDocProperty(String flavor, String property) {
  switch (flavor) {
    case 'admin-rstudio':
      switch (property) {
        case 'buildDir': return 'docs/desktop/'
        case 'uploadSrc': return 'docs/desktop/_site/'
        case 'uploadDst': return 'ide/desktop-pro'
        default: return null
      }
      break

    case 'admin-workbench':
      switch (property) {
        case 'buildDir': return 'docs/server/'
        case 'uploadSrc': return 'docs/server/_site/'
        case 'uploadDst': return 'ide/server-pro'
        default: return null
      }
      break

    case 'user-rstudio':
      switch (property) {
        case 'buildDir': return 'docs/user/rstudio/'
        case 'uploadSrc': return 'docs/user/rstudio/_site/'
        case 'uploadDst': return 'ide/user'
        default: return null
      }
      break

    case 'licenses':
      switch (property) {
        case 'buildDir': return 'docs/licenses/'
        case 'uploadSrc': return 'docs/licenses/_site/'
        case 'uploadDst': return 'ide/licenses'
        default: return null
      }
      break

    default:
      return null
  }
}

/**
 * Checks if this is a docs draft PR and returns its name
 * @param branchName the name of the branch being built
 * @return An empty string for a prod build, or the name of the draft build
 */
def getDocsDraftName(String branchName) {
  def DRAFT_PREFIX = 'docs-draft-'

  if (branchName.startsWith(DRAFT_PREFIX)) {
    return draftName = branchName.substring(DRAFT_PREFIX.length())
  }
  return ""
}


return this
