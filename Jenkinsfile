#!groovy

properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(artifactDaysToKeepStr: '',
                              artifactNumToKeepStr: '',
                              daysToKeepStr: '',
                              numToKeepStr: '100')),
    parameters([string(name: 'RSTUDIO_VERSION_MAJOR', defaultValue: '1', description: 'RStudio Major Version'),
                string(name: 'RSTUDIO_VERSION_MINOR', defaultValue: '3', description: 'RStudio Minor Version'),
                string(name: 'SLACK_CHANNEL', defaultValue: '#ide-builds', description: 'Slack channel to publish build message.'),
                string(name: 'OS_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching OS'),
                string(name: 'ARCH_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching ARCH'),
                string(name: 'FLAVOR_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching FLAVOR')
                ])
])

def compile_package(os, type, flavor, variant) {
  // start with major, minor, and patch versions
  def env = "RSTUDIO_VERSION_MAJOR=${rstudioVersionMajor} RSTUDIO_VERSION_MINOR=${rstudioVersionMinor} RSTUDIO_VERSION_PATCH=${rstudioVersionPatch}"

  // add version suffix if present
  if (rstudioVersionSuffix != 0) {
   env = "${env} RSTUDIO_VERSION_SUFFIX=${rstudioVersionSuffix}" 
  }

  // add OS that the package was built for
  env = "${env} PACKAGE_OS=\"${os}\""

  // currently our nodes have access to 4 cores, so spread out the compile job
  // a little (currently using up all 4 cores causes problems)
  env = "${env} MAKEFLAGS=-j3"

  // perform the compilation
  sh "cd package/linux && ${env} ./make-${flavor}-package ${type} clean ${variant} && cd ../.."

  // sign the new package
  withCredentials([file(credentialsId: 'gpg-codesign-private-key', variable: 'codesignKey'),
                   file(credentialsId: 'gpg-codesign-passphrase',  variable: 'codesignPassphrase')]) {
    sh "docker/jenkins/sign-release.sh package/linux/build-${flavor.capitalize()}-${type}/rstudio-*.${type.toLowerCase()} ${codesignKey} ${codesignPassphrase}"
  }
}

def run_tests(type, flavor, variant) {
  try {
    // attempt to run ant (gwt) unit tests
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/gwt && ./gwt-unit-tests.sh"
  } catch(err) {
    // mark build as unstable if it fails unit tests
    currentBuild.result = "UNSTABLE"
  }

  
  try {
    // attempt to run cpp unit tests
    // disable known broken tests for now (Jenkins cannot handle the parallel load these induce)
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/cpp && ./rstudio-tests --scope core"
  } catch(err) {
    currentBuild.result = "UNSTABLE"
  }
}

def s3_upload(type, flavor, os, arch) {
  // get package name from filesystem
  def buildFolder = "package/linux/build-${flavor.capitalize()}-${type}"
  def packageFile = sh (
    script: "basename `ls ${buildFolder}/rstudio-*.${type.toLowerCase()}`",
    returnStdout: true
  ).trim()

  // rename package to not include the build type
  def renamedFile = sh (
    script: "echo ${packageFile} | sed 's/-relwithdebinfo//'",
    returnStdout: true
  ).trim()

  sh "mv ${buildFolder}/${packageFile} ${buildFolder}/${renamedFile}"
  packageFile = renamedFile

  // copy installer to s3
  sh "aws s3 cp ${buildFolder}/${packageFile} s3://rstudio-ide-build/${flavor}/${os}/${arch}/"

  // add installer-less tarball if desktop
  if (flavor == "desktop") {
      sh "aws s3 cp ${buildFolder}/_CPack_Packages/Linux/${type}/*.tar.gz s3://rstudio-ide-build/${flavor}/${os}/${arch}/"
  }

  // update daily build redirect; currently disabled for 1.3
  // 
  // withCredentials([file(credentialsId: 'www-rstudio-org-pem', variable: 'wwwRstudioOrgPem')]) {
  //   sh "docker/jenkins/publish-daily-binary.sh https://s3.amazonaws.com/rstudio-ide-build/${flavor}/${os}/${arch}/${packageFile} ${wwwRstudioOrgPem}"
  // }
}

def sentry_upload(type, flavor) {
  withCredentials([string(credentialsId: 'ide-sentry-api-key', variable: 'SENTRY_API_KEY')]){
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/cpp && /usr/local/bin/sentry-cli --auth-token ${SENTRY_API_KEY} upload-dif --org rstudio --project ide-backend -t elf ."
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
  sh 'git reset --hard && git clean -ffdx' // lifted from rstudio/connect
}

// forward declare version vars
rstudioVersionMajor  = 0
rstudioVersionMinor  = 0
rstudioVersionPatch  = 0
rstudioVersionSuffix = 0

def trigger_external_build(build_name, wait = false) {
  // triggers downstream job passing along the important params from this build
  build job: build_name, wait: wait, parameters: [string(name: 'RSTUDIO_VERSION_MAJOR',  value: "${rstudioVersionMajor}"),
                                                  string(name: 'RSTUDIO_VERSION_MINOR',  value: "${rstudioVersionMinor}"),
                                                  string(name: 'RSTUDIO_VERSION_PATCH',  value: "${rstudioVersionPatch}"),
                                                  string(name: 'RSTUDIO_VERSION_SUFFIX', value: "${rstudioVersionSuffix}"),
                                                  string(name: 'SLACK_CHANNEL', value: SLACK_CHANNEL)]
}


// make a nicer slack message
messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>"

try {
    timestamps {
        def containers = [
          [os: 'centos6',    arch: 'x86_64', flavor: 'server',  variant: '',    package_os: 'CentOS 6'],
          [os: 'opensuse',   arch: 'x86_64', flavor: 'server',  variant: '',    package_os: 'OpenSUSE'],
          [os: 'opensuse',   arch: 'x86_64', flavor: 'desktop', variant: '',    package_os: 'OpenSUSE'],
          [os: 'opensuse15', arch: 'x86_64', flavor: 'desktop', variant: '',    package_os: 'OpenSUSE 15'],
          [os: 'opensuse15', arch: 'x86_64', flavor: 'server',  variant: '',    package_os: 'OpenSUSE 15'],
          [os: 'centos7',    arch: 'x86_64', flavor: 'desktop', variant: '',    package_os: 'CentOS 7'],
          [os: 'xenial',     arch: 'amd64',  flavor: 'server',  variant: '',    package_os: 'Ubuntu Xenial'],
          [os: 'xenial',     arch: 'amd64',  flavor: 'desktop', variant: '',    package_os: 'Ubuntu Xenial'],
          [os: 'bionic',     arch: 'amd64',  flavor: 'server',  variant: '',    package_os: 'Ubuntu Bionic'],
          [os: 'bionic',     arch: 'amd64',  flavor: 'desktop', variant: '',    package_os: 'Ubuntu Bionic'],
          [os: 'debian9',    arch: 'x86_64', flavor: 'server',  variant: '',    package_os: 'Debian 9'],
          [os: 'debian9',    arch: 'x86_64', flavor: 'desktop', variant: '',    package_os: 'Debian 9'],
          [os: 'centos8',   arch: 'x86_64', flavor: 'server',  variant: '',     package_os: 'CentOS 8'],
          [os: 'centos8',   arch: 'x86_64', flavor: 'desktop', variant: '',     package_os: 'CentOS 8']
        ]
        containers = limit_builds(containers)

        // build each container image
        parallel_images = [:]
        for (int i = 0; i < containers.size(); i++) {
            // derive the tag for this image
            def current_image = containers[i]
            def image_tag = "{current_image.os}-{current_image.arch}-${params.RSTUDIO_VERSION_MAJOR}.${params.RSTUDIO_VERSION_MINOR}"

            // ensure that this image tag has not already been built (since we
            // recycle tags for many platforms to e.g. build desktop and server
            // on the same image)
            if (!parallel_images.keySet().contains(image_tag)) {
                parallel_images[image_tag] = {
                    node('docker') {
                        stage('prepare container') {
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

        // create the version we're about to build
        node('docker') {
            stage('set up versioning') {
                prepareWorkspace()

                container = pullBuildPush(image_name: 'jenkins/ide', dockerfile: "docker/jenkins/Dockerfile.versioning", image_tag: "rstudio-versioning", build_args: jenkins_user_build_args())
                container.inside() {
                    stage('bump version') {
                        def rstudioVersion = sh (
                          script: "docker/jenkins/rstudio-version.sh bump ${params.RSTUDIO_VERSION_MAJOR}.${params.RSTUDIO_VERSION_MINOR}",
                          returnStdout: true
                        ).trim()
                        echo "RStudio build version: ${rstudioVersion}"
                        def components = rstudioVersion.split('\\.')

                        // extract major / minor version
                        rstudioVersionMajor = components[0]
                        rstudioVersionMinor = components[1]

                        // extract patch and suffix if present
                        def patch = components[2].split('-')
                        rstudioVersionPatch = patch[0]
                        if (patch.length > 1) 
                            rstudioVersionSuffix = patch[1]
                        else
                            rstudioVersionSuffix = 0

                        // update slack message to include build version
                        messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>, version: ${rstudioVersion}"
                    }
                }
            }
        }

        // build each variant in parallel
        def parallel_containers = [:]
        for (int i = 0; i < containers.size(); i++) {
            def index = i
            parallel_containers["${containers[i].os}-${containers[i].arch}-${containers[i].flavor}-${containers[i].variant}"] = {
                def current_container = containers[index]
                node('ide') {
                    def current_image
                    docker.withRegistry('https://263245908434.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:jenkins-aws') {
                        stage('prepare ws/container') {
                          prepareWorkspace()
                          def image_tag = "${current_container.os}-${current_container.arch}-${params.RSTUDIO_VERSION_MAJOR}.${params.RSTUDIO_VERSION_MINOR}"
                          current_image = docker.image("jenkins/ide:" + image_tag)
                        }
                        current_image.inside() {
                            stage('compile package') {
                                compile_package(current_container.package_os, get_type_from_os(current_container.os), current_container.flavor, current_container.variant)
                            }
                            stage('run tests') {
                                run_tests(get_type_from_os(current_container.os), current_container.flavor, current_container.variant)
                            }
                            stage('sentry upload') {
                                sentry_upload(get_type_from_os(current_container.os), current_container.flavor)
                            }
                        }
                    }
                    stage('upload artifacts') {
                        s3_upload(get_type_from_os(current_container.os), current_container.flavor, current_container.os, current_container.arch)
                    }
                }
            }
        }
        // trigger desktop builds if we're in open-source repo
        if (env.JOB_NAME == 'IDE/open-source-pipeline/master') {
          trigger_external_build('IDE/macos-v1.3')
          trigger_external_build('IDE/windows-v1.3')
        }
        else if (env.JOB_NAME == 'IDE/open-source-pipeline/v1.2-patch') {
          trigger_external_build('IDE/macos-v1.2')
          trigger_external_build('IDE/windows-v1.2')
        }
        parallel parallel_containers

        // trigger downstream pro artifact builds if we're finished building
        // the pro variants
        // additionally, run qa-autotest against the version we've just built
        if (env.JOB_NAME == 'IDE/pro-pipeline/master') {
          trigger_external_build('IDE/pro-docs')
          trigger_external_build('IDE/launcher-docs')
          trigger_external_build('IDE/pro-desktop-docs')
          trigger_external_build('IDE/qa-autotest')
          trigger_external_build('IDE/qa-automation')
          trigger_external_build('IDE/monitor')
          trigger_external_build('IDE/macos-v1.3-pro')
          trigger_external_build('IDE/windows-v1.3-pro')
          trigger_external_build('IDE/session')
        }
        else if (env.JOB_NAME == 'IDE/pro-pipeline/v1.2') {
          trigger_external_build('IDE/macos-v1.2-pro')
          trigger_external_build('IDE/windows-v1.2-pro')
        }

        slackSend channel: params.get('SLACK_CHANNEL', '#ide-builds'), color: 'good', message: "${messagePrefix} passed (${currentBuild.result})"
    }

} catch(err) {
   slackSend channel: params.get('SLACK_CHANNEL', '#ide-builds'), color: 'bad', message: "${messagePrefix} failed: ${err}"
   error("failed: ${err}")
}
