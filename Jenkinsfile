#!groovy

properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(artifactDaysToKeepStr: '',
                              artifactNumToKeepStr: '',
                              daysToKeepStr: '',
                              numToKeepStr: '100')),
    parameters([string(name: 'RSTUDIO_VERSION_PATCH', defaultValue: '0', description: 'RStudio Patch Version'),
                string(name: 'SLACK_CHANNEL', defaultValue: '#ide-builds', description: 'Slack channel to publish build message.'),
                string(name: 'OS_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching OS'),
                string(name: 'ARCH_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching ARCH'),
                string(name: 'FLAVOR_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching FLAVOR')
                ])
])

def compile_package(os, type, flavor, variant) {
  // start with major, minor, and patch versions
  def envVars = "RSTUDIO_VERSION_MAJOR=${rstudioVersionMajor} RSTUDIO_VERSION_MINOR=${rstudioVersionMinor} RSTUDIO_VERSION_PATCH=${rstudioVersionPatch} RSTUDIO_VERSION_SUFFIX=${rstudioVersionSuffix}"

  // add OS that the package was built for
  envVars = "${envVars} PACKAGE_OS=\"${os}\""

  // currently our nodes have access to 4 cores, so spread out the compile job
  // a little (currently using up all 4 cores causes problems)
  envVars = "${envVars} MAKEFLAGS=-j2"

  // perform the compilation
  sh "cd package/linux && ${envVars} ./make-${flavor}-package ${type} clean ${variant} && cd ../.."

  // sign the new package
  withCredentials([file(credentialsId: 'gpg-codesign-private-key', variable: 'codesignKey'),
                   file(credentialsId: 'gpg-codesign-passphrase',  variable: 'codesignPassphrase')]) {
    sh "docker/jenkins/sign-release.sh package/linux/build-${flavor.capitalize()}-${type}/rstudio-*.${type.toLowerCase()} ${codesignKey} ${codesignPassphrase}"
  }
}

def run_tests(os, type, flavor) {
  try {
    // attempt to run ant (gwt) unit tests
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/gwt && ./gwt-unit-tests.sh"
  } catch(err) {
    // mark build as unstable if it fails unit tests
    unstable("GWT unit tests failed (${flavor.capitalize()} ${type} on ${os})")
  }


  try {
    // attempt to run cpp unit tests
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/cpp && ./rstudio-tests"
  } catch(err) {
    unstable("C++ unit tests failed (${flavor.capitalize()} ${type} on ${os})")
  }
}

def jenkins_user_build_args() {
  def jenkins_uid = sh (script: 'id -u jenkins', returnStdout: true).trim()
  def jenkins_gid = sh (script: 'id -g jenkins', returnStdout: true).trim()
  return " --build-arg JENKINS_UID=${jenkins_uid} --build-arg JENKINS_GID=${jenkins_gid}"
}

def get_type_from_os(os) {
  def type
  // groovy switch case regex is broken in pipeline
  // https://issues.jenkins-ci.org/browse/JENKINS-37214
  if (os.contains('centos') || os.contains('suse') || os.contains('fedora')) {
    type = 'RPM'
  } else {
    type = 'DEB'
  }
  return type
}

def limit_builds(containers) {
  // '' (empty string) as regex matches all
  def limited_containers = []
  for (int i = 0; i < containers.size(); i++) {
    def it = containers[i]
    // negate-fest. String.contains() can't work in the positive with empty strings
    if (!(!it.os.contains(params.OS_FILTER) || !it.arch.contains(params.ARCH_FILTER) || !it.flavor.contains(params.FLAVOR_FILTER))) {
      limited_containers << it
    }
  }
  return limited_containers ?: containers // if we limit all, limit none
}

def prepareWorkspace(){ // accessory to clean workspace and checkout
  step([$class: 'WsCleanup'])
  checkout scm
  // record the commit for invoking downstream builds
  rstudioBuildCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
  sh 'git reset --hard && git clean -ffdx' // lifted from rstudio/connect
}

// forward declare version vars
rstudioVersionMajor  = 0
rstudioVersionMinor  = 0
rstudioVersionPatch  = 0
rstudioVersionSuffix = 0
rstudioBuildCommit   = ""

// compute release branch by parsing the job name (env.GIT_BRANCH should work here but doesn't appear to), e.g.
// "IDE/pro-pipeline/v4.3" => "v4.3"
branchComponents = env.JOB_NAME.split("/")
rstudioReleaseBranch = "main" //pretend to be main

def trigger_external_build(build_name, wait = false) {
  // triggers downstream job passing along the important params from this build
  build job: build_name, wait: wait, parameters: [string(name: 'RSTUDIO_VERSION_MAJOR',  value: "${rstudioVersionMajor}"),
                                                  string(name: 'RSTUDIO_VERSION_MINOR',  value: "${rstudioVersionMinor}"),
                                                  string(name: 'RSTUDIO_VERSION_PATCH',  value: "${rstudioVersionPatch}"),
                                                  string(name: 'RSTUDIO_VERSION_SUFFIX', value: "${rstudioVersionSuffix}"),
                                                  string(name: 'GIT_REVISION', value: "${rstudioBuildCommit}"),
                                                  string(name: 'BRANCH_NAME', value: "${rstudioReleaseBranch}"),
                                                  string(name: 'SLACK_CHANNEL', value: SLACK_CHANNEL)]
}


// make a nicer slack message
messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>"

try {

    timestamps {
        def containers = [
          [os: 'opensuse',   arch: 'x86_64', flavor: 'server',  variant: '',    package_os: 'OpenSUSE']
        ]
        containers = limit_builds(containers)

        // create the version we're about to build
        node('docker') {
            stage('set up versioning') {
                prepareWorkspace()

                container = pullBuildPush(image_name: 'jenkins/ide', dockerfile: "docker/jenkins/Dockerfile.versioning", image_tag: "rstudio-versioning", build_args: jenkins_user_build_args())
                container.inside() {
                    stage('bump version') {
                        def rstudioVersion = sh (
                          script: "docker/jenkins/rstudio-version.sh bump ${params.RSTUDIO_VERSION_PATCH}",
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
                        if (version.length > 2)
                            rstudioVersionSuffix = '-' + version[1] + '+' + version[2]
                        else
                            rstudioVersionSuffix = '+' + version[1]

                        // update slack message to include build version
                        messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>, version: ${rstudioVersion}"
                    }
                }
            }
        }

        // build each container image
        parallel_images = [:]
        for (int i = 0; i < containers.size(); i++) {
            // derive the tag for this image
            def current_image = containers[i]
            def image_tag = "${current_image.os}-${current_image.arch}-${rstudioReleaseBranch}"

            // ensure that this image tag has not already been built (since we
            // recycle tags for many platforms to e.g. build desktop and server
            // on the same image)
            if (!parallel_images.keySet().contains(image_tag)) {
                parallel_images[image_tag] = {
                    node('docker') {
                        stage('prepare Linux container') {
                            prepareWorkspace()
                            withCredentials([usernameColonPassword(credentialsId: 'github-rstudio-jenkins', variable: "github_login")]) {
                              def github_args = "--build-arg GITHUB_LOGIN=${github_login}"
                              pullBuildPush(image_name: 'jenkins/ide',
                                dockerfile: "docker/jenkins/Dockerfile.${current_image.os}-${current_image.arch}",
                                image_tag: image_tag,
                                build_args: github_args + " " + jenkins_user_build_args())
                            }
                        }
                    }
                }
            }
        }

        parallel parallel_images

        def parallel_containers = [:]

        // build each variant in parallel
        for (int i = 0; i < containers.size(); i++) {
            def index = i
            parallel_containers["${containers[i].os}-${containers[i].arch}-${containers[i].flavor}-${containers[i].variant}"] = {
                def current_container = containers[index]
                node('ide') {
                    def current_image
                    docker.withRegistry('https://263245908434.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:jenkins-aws') {
                        stage('prepare ws/container') {
                          prepareWorkspace()
                          def image_tag = "${current_container.os}-${current_container.arch}-${rstudioReleaseBranch}"
                          current_image = docker.image("jenkins/ide:" + image_tag)
                        }
                        current_image.inside("--privileged") {
                            stage('compile package') {
                                compile_package(current_container.package_os, get_type_from_os(current_container.os), current_container.flavor, current_container.variant)
                            }
                            stage('run tests') {
                                run_tests(current_container.os, get_type_from_os(current_container.os), current_container.flavor)
                            }
                        }
                    }
                }
            }
        }
    }
} catch(err) {
  //Swallow
}
