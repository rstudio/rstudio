#!groovy

properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(artifactDaysToKeepStr: '',
                              artifactNumToKeepStr: '',
                              daysToKeepStr: '',
                              numToKeepStr: '100')),
    parameters([string(name: 'RSTUDIO_VERSION_MAJOR', defaultValue: '1', description: 'RStudio Major Version'),
                string(name: 'RSTUDIO_VERSION_MINOR', defaultValue: '1', description: 'RStudio Minor Version'),
                string(name: 'RSTUDIO_VERSION_PATCH', defaultValue: '0', description: 'RStudio Patch Version'),
                string(name: 'SLACK_CHANNEL', defaultValue: '#ide-builds', description: 'Slack channel to publish build message.'),
                string(name: 'OS_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching OS'),
                string(name: 'ARCH_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching ARCH'),
                string(name: 'FLAVOR_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching FLAVOR')
                ])
])

def resolve_deps(type, arch, variant) {
  def linux_bin = (arch == 'i386') ? 'linux32' : '' // only required in centos-land.
  switch ( type ) {
      case "DEB":
        sh "cd dependencies/linux && ./install-dependencies-debian --exclude-qt-sdk && cd ../.."
        break
      case "RPM":
        sh "cd dependencies/linux && ${linux_bin} ./install-dependencies-yum --exclude-qt-sdk && cd ../.."
  }
}

def compile_package(type, flavor, variant) {
  def env = "RSTUDIO_VERSION_MAJOR=${RSTUDIO_VERSION_MAJOR} RSTUDIO_VERSION_MINOR=${RSTUDIO_VERSION_MINOR} RSTUDIO_VERSION_PATCH=${RSTUDIO_VERSION_PATCH}"
  sh "cd package/linux && ${env} ./make-${flavor}-package ${type} clean ${variant} && cd ../.."
}

def s3_upload(type, flavor, os, arch) {
  sh "aws s3 cp package/linux/build-${flavor.capitalize()}-${type}/rstudio-*.${type.toLowerCase()} s3://rstudio-ide-build/${flavor}/${os}/${arch}/"
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
  if (os.contains('centos') || os.contains('suse')) {
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
    if (!(!it.os.contains(OS_FILTER) || !it.arch.contains(ARCH_FILTER) || !it.flavor.contains(FLAVOR_FILTER))) {
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

def trigger_external_build(build_name, wait = false) {
  // triggers downstream job passing along the important params from this build
  build job: build_name, wait: wait, parameters: [string(name: 'RSTUDIO_VERSION_MAJOR', value: RSTUDIO_VERSION_MAJOR),
                                                  string(name: 'RSTUDIO_VERSION_MINOR', value: RSTUDIO_VERSION_MINOR),
                                                  string(name: 'RSTUDIO_VERSION_PATCH', value: RSTUDIO_VERSION_PATCH),
                                                  string(name: 'SLACK_CHANNEL', value: SLACK_CHANNEL)]
}

// make a nicer slack message
rstudioVersion = "${RSTUDIO_VERSION_MAJOR}.${RSTUDIO_VERSION_MINOR}.${RSTUDIO_VERSION_PATCH}"
messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>, version: ${rstudioVersion}"

try {
    timestamps {
        def containers = [
          [os: 'precise',  arch: 'amd64',  flavor: 'desktop', variant: 'trusty'],
          [os: 'precise',  arch: 'i386',   flavor: 'desktop', variant: 'trusty'],
          [os: 'precise',  arch: 'amd64',  flavor: 'server',  variant: ''],
          [os: 'precise',  arch: 'i386',   flavor: 'server',  variant: ''],
          [os: 'centos6',  arch: 'x86_64', flavor: 'server',  variant: ''],
          [os: 'centos6',  arch: 'i386',   flavor: 'server',  variant: ''],
          [os: 'centos6',  arch: 'x86_64', flavor: 'server',  variant: 'SLES'],
          [os: 'centos6',  arch: 'i386',   flavor: 'server',  variant: 'SLES'],
          [os: 'centos7',  arch: 'x86_64', flavor: 'desktop', variant: ''],
          [os: 'centos7',  arch: 'i386',   flavor: 'desktop', variant: ''],
          [os: 'xenial',   arch: 'amd64',  flavor: 'desktop', variant: 'xenial'],
          [os: 'xenial',   arch: 'i386',   flavor: 'desktop', variant: 'xenial'],
          [os: 'xenial',   arch: 'amd64',  flavor: 'server', variant: 'xenial'],
          [os: 'xenial',   arch: 'i386',   flavor: 'server', variant: 'xenial'],
          [os: 'debian9',  arch: 'x86_64', flavor: 'server', variant: 'stretch']
        ]
        containers = limit_builds(containers)
        // launch jenkins agents to support the container scale!
        spotScaleSwarm layer_name: 'swarm-ide', instance_count: containers.size(), duration_seconds: 7000
        def parallel_containers = [:]
        for (int i = 0; i < containers.size(); i++) {
            def index = i
            parallel_containers["${containers[i].os}-${containers[i].arch}-${containers[i].flavor}-${containers[i].variant}"] = {
                def current_container = containers[index]
                node('ide') {
                    stage('prepare ws/container'){
                      prepareWorkspace()
                      def image_tag = "${current_container.os}-${current_container.arch}-${RSTUDIO_VERSION_MAJOR}.${RSTUDIO_VERSION_MINOR}"
                      container = pullBuildPush(image_name: 'jenkins/ide', dockerfile: "docker/jenkins/Dockerfile.${current_container.os}-${current_container.arch}", image_tag: image_tag, build_args: jenkins_user_build_args())
                    }
                    container.inside() {
                        stage('resolve deps'){
                            resolve_deps(get_type_from_os(current_container.os), current_container.arch, current_container.variant)
                        }
                        stage('compile package') {
                            compile_package(get_type_from_os(current_container.os), current_container.flavor, current_container.variant)
                        }
                    }
                    stage('upload artifacts') {
                        s3_upload(get_type_from_os(current_container.os), current_container.flavor, current_container.os, current_container.arch)
                    }
                }
            }
        }
        // trigger macos build if we're in open-source repo
        if (env.JOB_NAME == 'IDE/open-source') {
          trigger_external_build('IDE/macos')
          trigger_external_build('IDE/windows')
        }
        parallel parallel_containers

        // trigger downstream pro-docs build if we're finished building the pro variants
        // additionally, run qa-autotest against the version we've just built
        if (env.JOB_NAME == 'IDE/pro') {
          trigger_external_build('IDE/pro-docs')
          trigger_external_build('IDE/qa-autotest')
          trigger_external_build('IDE/monitor')
        }

        slackSend channel: SLACK_CHANNEL, color: 'good', message: "${messagePrefix} passed"
    }

} catch(err) {
   slackSend channel: SLACK_CHANNEL, color: 'bad', message: "${messagePrefix} failed: ${err}"
   error("failed: ${err}")
}
