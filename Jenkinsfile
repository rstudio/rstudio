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
                string(name: 'SLACK_CHANNEL', defaultValue: '#rstudio', description: 'Slack channel to publish build message.'),
                string(name: 'OS_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching OS'),
                string(name: 'ARCH_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching ARCH'),
                string(name: 'FLAVOR_FILTER', defaultValue: '', description: 'Pattern to limit builds by matching FLAVOR')
                ])
])

def resolve_deps(type, arch) {
  def linux_bin = (arch=='i386') ? 'linux32' : '' // only required in centos-land.
  switch ( type ) {
      case "DEB":
        sh "cd dependencies/linux && ./install-dependencies-debian --exclude-qt-sdk && cd ../.."
        break
      case "RPM":
        sh "cd dependencies/linux && ${linux_bin} ./install-dependencies-yum --exclude-qt-sdk && cd ../.."
        break
  }
}

def compile_package(type, flavor) {
  def env = "RSTUDIO_VERSION_MAJOR=${RSTUDIO_VERSION_MAJOR} RSTUDIO_VERSION_MINOR=${RSTUDIO_VERSION_MINOR} RSTUDIO_VERSION_PATCH=${RSTUDIO_VERSION_PATCH}"
  sh "cd package/linux && ${env} ./make-${flavor}-package ${type} clean && cd ../.."
}

def s3_upload(type, flavor, os, arch) {
  sh "aws s3 cp package/linux/build-${flavor.capitalize()}-${type}/rstudio-*.${type.toLowerCase()} s3://rstudio-ide-build/${flavor}/${os}/${arch}/"
}

def pull_build_push(current_container) {
  def image_tag = "${current_container.os}-${current_container.arch}-${RSTUDIO_VERSION_MAJOR}.${RSTUDIO_VERSION_MINOR}"
  def image_cache
  try {
    image_cache = docker.image("jenkins/ide:" + image_tag)
    image_cache.pull()
  } catch(e) {
    // assume this is "image not found" and simply build.
  }
  def jenkins_uid = sh (script: 'id -u jenkins', returnStdout: true).trim()
  def jenkins_gid = sh (script: 'id -g jenkins', returnStdout: true).trim()
  def build_args = "--cache-from ${image_cache.imageName()} --build-arg JENKINS_UID=${jenkins_uid} --build-arg JENKINS_GID=${jenkins_gid}"
  def container = docker.build("jenkins/ide:" + image_tag, "${build_args} -f docker/jenkins/Dockerfile.${current_container.os}-${current_container.arch} .")
  container.push()
  return container
}

def get_type_from_os(os) {
  def type
  // groovy switch case regex is broken in pipeline
  // https://issues.jenkins-ci.org/browse/JENKINS-37214
  if (os.contains('centos')) {
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

// make a nicer slack message
rstudioVersion = "${RSTUDIO_VERSION_MAJOR}.${RSTUDIO_VERSION_MINOR}.${RSTUDIO_VERSION_PATCH}"
messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>, version: ${rstudioVersion}"

try {
    timestamps {
        def containers = [
          [os: 'precise', arch: 'amd64', flavor: 'desktop'],
          [os: 'precise', arch: 'i386', flavor: 'desktop'],
          [os: 'precise', arch: 'amd64', flavor: 'server'],
          [os: 'precise', arch: 'i386', flavor: 'server'],
          [os: 'centos6', arch: 'x86_64', flavor: 'server'],
          [os: 'centos6', arch: 'i386', flavor: 'server'],
          //[os: 'centos5', arch: 'x86_64', flavor: 'server'],
          //[os: 'centos5', arch: 'i386', flavor: 'server'],
          [os: 'centos7', arch: 'x86_64', flavor: 'desktop'],
          [os: 'centos7', arch: 'i386', flavor: 'desktop'],
          [os: 'xenial', arch: 'amd64', flavor: 'server'],
          [os: 'xenial', arch: 'i386', flavor: 'server']
        ]
        containers = limit_builds(containers)
        def parallel_containers = [:]
        for (int i = 0; i < containers.size(); i++) {
            def index = i
            parallel_containers["${containers[i].os}-${containers[i].arch}-${containers[i].flavor}"] = {
                def current_container = containers[index]
                node('ide') {
                    docker.withRegistry('https://263245908434.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:jenkins-aws') {
                      stage('prepare ws/container'){
                        prepareWorkspace()
                        container = pull_build_push(current_container)
                      }
                      container.inside() {
                          stage('resolve deps'){
                              resolve_deps(get_type_from_os(current_container.os), current_container.arch)
                          }
                          stage('compile package') {
                              compile_package(get_type_from_os(current_container.os), current_container.flavor)
                          }
                      }
                    }
                    stage('upload artifacts') {
                        s3_upload(get_type_from_os(current_container.os), current_container.flavor, current_container.os, current_container.arch)
                    }
                }
            }
        }
        parallel parallel_containers
        slackSend channel: SLACK_CHANNEL, color: 'good', message: "${messagePrefix} passed"
    }

} catch(err) {
   slackSend channel: SLACK_CHANNEL, color: 'bad', message: "${messagePrefix} failed: ${err}"
   error("failed: ${err}")
}
