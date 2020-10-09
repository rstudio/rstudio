#!groovy

properties([
    disableConcurrentBuilds(),
    buildDiscarder(logRotator(artifactDaysToKeepStr: '',
                              artifactNumToKeepStr: '',
                              daysToKeepStr: '',
                              numToKeepStr: '100')),
    parameters([string(name: 'RSTUDIO_VERSION_MAJOR', defaultValue: '1', description: 'RStudio Major Version'),
                string(name: 'RSTUDIO_VERSION_MINOR', defaultValue: '4', description: 'RStudio Minor Version'),
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
  env = "${env} MAKEFLAGS=-j2"

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
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/cpp && ./rstudio-tests"
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
    def tarballFile = sh (
      script: "basename `ls ${buildFolder}/_CPack_Packages/Linux/${type}/*.tar.gz`",
      returnStdout: true
    ).trim()
    
    def renamedTarballFile = sh (
      script: "echo ${tarballFile} | sed 's/-relwithdebinfo//'",
      returnStdout: true
    ).trim()

    sh "mv ${buildFolder}/_CPack_Packages/Linux/${type}/${tarballFile} ${buildFolder}/_CPack_Packages/Linux/${type}/${renamedTarballFile}"
    tarballFile = renamedTarballFile
    
    sh "aws s3 cp ${buildFolder}/_CPack_Packages/Linux/${type}/${tarballFile} s3://rstudio-ide-build/${flavor}/${os}/${arch}/"
  }

  // update daily build redirect
  withCredentials([file(credentialsId: 'www-rstudio-org-pem', variable: 'wwwRstudioOrgPem')]) {
    sh "docker/jenkins/publish-daily-binary.sh https://s3.amazonaws.com/rstudio-ide-build/${flavor}/${os}/${arch}/${packageFile} ${wwwRstudioOrgPem}"
  }
}

def sentry_upload(type, flavor) {
  withCredentials([string(credentialsId: 'ide-sentry-api-key', variable: 'SENTRY_API_KEY')]){
    sh "cd package/linux/build-${flavor.capitalize()}-${type}/src/cpp && ../../../../../docker/jenkins/sentry-upload.sh ${SENTRY_API_KEY}"
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

        // build each container image
        parallel_images = [:]
        for (int i = 0; i < containers.size(); i++) {
            // derive the tag for this image
            def current_image = containers[i]
            def image_tag = "${current_image.os}-${current_image.arch}-${params.RSTUDIO_VERSION_MAJOR}.${params.RSTUDIO_VERSION_MINOR}"

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

        // prepare container for windows builder
        parallel_images["windows"] = {
          node('windows') {
            stage('prepare Windows container') {
              checkout scm
              withCredentials([usernameColonPassword(credentialsId: 'github-rstudio-jenkins', variable: "github_login")]) {
                def github_args = "--build-arg GITHUB_LOGIN=${github_login}"
                def dockerfile = "-f docker/jenkins/Dockerfile.windows"
                def container
                // the following is adapted from pullBuildPush with the
                // omission of Unix-isms
                docker.withRegistry('https://263245908434.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:jenkins-aws') {
                  def image_cache
                  def image_name = "jenkins/ide"
                  def image_tag = "windows-${params.RSTUDIO_VERSION_MAJOR}.${params.RSTUDIO_VERSION_MINOR}"
                  def cache_tag = image_tag
                  def build_args = github_args
                  def docker_context = '.'
                  try {
                    image_cache = docker.image(image_name + ':' + cache_tag)
                    image_cache.pull()
                  } catch(e) { // docker.image throws a generic exception.
                    echo 'Windows container image not found; expect build to take a bit longer.'
                  }

                  echo 'Building Windows container image'
                  container = docker.build(image_name + ':' + image_tag, "--cache-from ${image_cache.imageName()} ${build_args} ${dockerfile} ${docker_context}")

                  echo 'Pushing Windows container'
                  container.push()
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
                          def image_tag = "${current_container.os}-${current_container.arch}-${params.RSTUDIO_VERSION_MAJOR}.${params.RSTUDIO_VERSION_MINOR}"
                          current_image = docker.image("jenkins/ide:" + image_tag)
                        }
                        current_image.inside("--privileged") {
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

        parallel_containers["windows"] = {
          node('windows') {
            stage('prepare container') {
               checkout scm
               docker.withRegistry('https://263245908434.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:jenkins-aws') {
                 def image_tag = "windows-${rstudioVersionMajor}.${rstudioVersionMinor}"
                 windows_image = docker.image("jenkins/ide:" + image_tag)
               }
            }
            windows_image.inside() {
              stage('dependencies') {
                  withCredentials([usernameColonPassword(credentialsId: 'github-rstudio-jenkins', variable: "GITHUB_LOGIN")]) {
                    bat 'cd dependencies/windows && set RSTUDIO_GITHUB_LOGIN=$GITHUB_LOGIN && set RSTUDIO_SKIP_QT=1 && install-dependencies.cmd && cd ../..'
                }
              }
              stage('build'){
                def env = "set \"RSTUDIO_VERSION_MAJOR=${rstudioVersionMajor}\" && set \"RSTUDIO_VERSION_MINOR=${rstudioVersionMinor}\" && set \"RSTUDIO_VERSION_PATCH=${rstudioVersionPatch}\""
                bat "cd package/win32 && ${env} && set \"PACKAGE_OS=Windows\" && make-package.bat clean && cd ../.."
              }
              stage('tests'){
                try {
                  bat 'cd package/win32/build/src/cpp && rstudio-tests.bat --scope core'
                }
                catch(err){
                  currentBuild.result = "UNSTABLE"
                }
              }
              stage('sign') {
                withCredentials([file(credentialsId: 'ide-windows-signing-pfx', variable: 'pfx-file'), string(credentialsId: 'ide-pfx-passphrase', variable: 'pfx-passphrase')]) {
                  bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17134.0\\x86\\signtool\" sign /f %pfx-file% /p %pfx-passphrase% /v /ac package\\win32\\cert\\After_10-10-10_MSCV-VSClass3.cer /n \"RStudio, Inc.\" /t http://timestamp.VeriSign.com/scripts/timstamp.dll  package\\win32\\build\\RStudio-${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}-RelWithDebInfo.exe"
                  bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17134.0\\x86\\signtool\" verify /v /kp package\\win32\\build\\RStudio-${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}-RelWithDebInfo.exe"
                }
              }
              stage('upload debug symbols') {
                // convert the PDB symbols to breakpad format (PDB not supported by Sentry)
                bat '''
                  cd package\\win32\\build
                  FOR /F %%G IN ('dir /s /b *.pdb') DO (..\\..\\..\\dependencies\\windows\\breakpad-tools-windows\\dump_syms %%G > %%G.sym)
                '''
                
                // upload the breakpad symbols
                withCredentials([string(credentialsId: 'ide-sentry-api-key', variable: 'SENTRY_API_KEY')]){
                  bat "cd package\\win32\\build\\src\\cpp && ..\\..\\..\\..\\..\\dependencies\\windows\\sentry-cli.exe --auth-token %SENTRY_API_KEY% upload-dif --org rstudio --project ide-backend -t breakpad ."
                }
              }
              stage('upload') {
                // windows docker container cannot reach instance-metadata endpoint. supply credentials at upload.
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'jenkins-aws']]) {
                  bat "aws s3 cp package\\win32\\build\\RStudio-${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}-RelWithDebInfo.exe s3://rstudio-ide-build/desktop/windows/RStudio-${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}.exe"
                  bat "aws s3 cp package\\win32\\build\\RStudio-${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}-RelWithDebInfo.zip s3://rstudio-ide-build/desktop/windows/RStudio-${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}.zip"
                }
              }
            }
          }
        }

        // trigger macos build if we're in open-source repo
        if (env.JOB_NAME == 'IDE/open-source-pipeline/master') {
          trigger_external_build('IDE/macos-v1.4')
        }

        else if (env.JOB_NAME == 'IDE/open-source-pipeline/v1.3') {
          trigger_external_build('IDE/macos-v1.3')
        }
        parallel parallel_containers

        if (env.JOB_NAME == 'IDE/open-source-pipeline/master') {
          trigger_external_build('IDE/qa-opensource-automation')
        }

        // trigger downstream pro artifact builds if we're finished building
        // the pro variants
        // additionally, run qa-autotest against the version we've just built
        if (env.JOB_NAME == 'IDE/pro-pipeline/master') {
          trigger_external_build('IDE/pro-docs-v1.4')
          trigger_external_build('IDE/launcher-docs-v1.4')
          trigger_external_build('IDE/pro-desktop-docs-v1.4')
          trigger_external_build('IDE/qa-autotest')
          trigger_external_build('IDE/qa-automation')
          trigger_external_build('IDE/monitor-v1.4')
          trigger_external_build('IDE/macos-v1.4-pro')
          trigger_external_build('IDE/windows-v1.4-pro')
          trigger_external_build('IDE/session-v1.4')
        }

        slackSend channel: params.get('SLACK_CHANNEL', '#ide-builds'), color: 'good', message: "${messagePrefix} passed (${currentBuild.result})"
    }

} catch(err) {
   slackSend channel: params.get('SLACK_CHANNEL', '#ide-builds'), color: 'bad', message: "${messagePrefix} failed: ${err}"
   error("failed: ${err}")
}

