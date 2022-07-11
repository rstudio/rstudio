#!groovy
@Library('pipeline-shared-libraries@feature/windows-pbp') _

rstudioVersionMajor = 0
rstudioVersionMinor = 0
rstudioVersionPatch = 0
rstudioVersionSuffix = ''
versionWithoutPlus = ''

def trigger_external_build(build_name) {
  def branchName = env.BRANCH_NAME.replaceAll('/', '%252F')

  build job: "${build_name}/${branchName}", wait: false, parameters: [
    string(name: 'RSTUDIO_VERSION_MAJOR',  value: "${rstudioVersionMajor}"),
    string(name: 'RSTUDIO_VERSION_MINOR',  value: "${rstudioVersionMinor}"),
    string(name: 'RSTUDIO_VERSION_PATCH',  value: "${rstudioVersionPatch}"),
    string(name: 'RSTUDIO_VERSION_SUFFIX', value: "${rstudioVersionSuffix}")
  ]
}

pipeline {
  agent none

  options {
    disableConcurrentBuilds()
    buildDiscarder(
      logRotator(
        artifactDaysToKeepStr: '',
        artifactNumToKeepStr: '',
        daysToKeepStr: '',
        numToKeepStr: '100'))
  }

  parameters {
    string(name: 'RSTUDIO_VERSION_PATCH', defaultValue: '0', description: 'RStudio Patch Version')
    string(name: 'SLACK_CHANNEL', defaultValue: '#ide-builds', description: 'Slack channel to publish build message.')
    string(name: 'OS_FILTER', defaultValue: 'all', description: 'Pattern to limit builds by matching OS')
    string(name: 'ARCH_FILTER', defaultValue: 'all', description: 'Pattern to limit builds by matching ARCH')
    string(name: 'FLAVOR_FILTER', defaultValue: 'all', description: 'Pattern to limit builds by matching FLAVOR')
  }

  stages {
    stage('Build Versioning Image') {
      agent {
        label 'linux && amd64'
      }

      steps {
        pullBuildPush(
          image_name: 'jenkins/ide',
          image_tag: 'rstudio-versioning',
          latest_tag: false,
          dockerfile: 'docker/jenkins/Dockerfile.versioning',
          build_arg_jenkins_uid: 'JENKINS_UID',
          build_arg_jenkins_gid: 'JENKINS_GID',
          push: env.BRANCH_IS_PRIMARY
        )
      }
    }

    stage('Bump Version') {
      agent {
        docker {
          image 'jenkins/ide:rstudio-versioning'
          registryCredentialsId 'ecr:us-east-1:aws-build-role'
          registryUrl 'https://263245908434.dkr.ecr.us-east-1.amazonaws.com'
          reuseNode true
        }
      }

      steps {
        script {
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
          versionWithoutPlus = majorComponents

          // Extract suffix
          if (version.length > 2) {
            rstudioVersionSuffix = '-' + version[1] + '+' + version[2]
            versionWithoutPlus = '-' + version[1] + '-' + version[2]
          }
          else {
            rstudioVersionSuffix = '+' + version[1]
            versionWithoutPlus = '-' + version[1]
          }

          // update slack message to include build version
          messagePrefix = "Jenkins ${env.JOB_NAME} build: <${env.BUILD_URL}display/redirect|${env.BUILD_DISPLAY_NAME}>, version: ${rstudioVersion}"                    
        }                
      }
    }

    stage('Start External Jobs') {
      agent {
        label 'linux'
      }
      steps {
          // Upload the version file for tests
          archiveArtifacts artifacts: 'version/RELEASE', followSymlinks: false

          script {
            // Start external build jobs
            if (env.JOB_NAME.startsWith('IDE/open-source-pipeline')) {
              trigger_external_build('IDE/macos-pipeline-test')
            }
          }
      }
    }

    stage('Container Matrix') {
      matrix {
        agent {
          label "${agent_label} && ${arch}"
        }

        when {
          anyOf {
            equals expected: params.OS_FILTER, actual: env.os;
            equals expected: params.OS_FILTER, actual: 'all'
          }
        }

        axes {
          axis {
            name 'agent_label'
            values 'linux', 'windows'
          }
          axis {
            name 'os'
            values 'bionic', 'centos7', 'debian9', 'rhel8', 'opensuse15', 'windows'
          }
          axis {
            name 'arch'
            values 'amd64'
          }
        }

        excludes {
          exclude {
            axis {
              name 'agent_label'
              values 'windows'
            }
            axis {
              name 'os'
              notValues 'windows'
            }
          }
          exclude {
            axis {
              name 'agent_label'
              values 'linux'
            }
            axis {
              name 'os'
              values 'windows'
            }
          }
        }
        stages {
          stage('Prepare Container') {
            environment {
              GITHUB_LOGIN = credentials('github-rstudio-jenkins')
            }

            steps {
              pullBuildPush(
                image_name: 'jenkins/ide',
                image_tag: "${os}-${arch}-${env.BRANCH_NAME.replaceAll('/', '-')}",
                latest_tag: false,
                dockerfile: "docker/jenkins/Dockerfile.${os}-${arch}",
                build_arg_jenkins_uid: 'JENKINS_UID',
                build_arg_jenkins_gid: 'JENKINS_GID',
                builds_args: "--build-arg GITHUB_LOGIN=${GITHUB_LOGIN}",
                push: env.BRANCH_IS_PRIMARY
              )
            }
          }
        }
      }
    }

    stage('Build Matrix') {
      matrix {
        agent {
          label "${agent_label} && ${arch}"
        }

        when {
          anyOf {
            equals expected: params.OS_FILTER, actual: env.os;
            equals expected: params.OS_FILTER, actual: 'all'
          }
        }

        axes {
          axis {
            name 'agent_label'
            values 'linux', 'windows'
          }
          axis {
              name 'os'
              values 'bionic', 'centos7', 'debian9', 'rhel8', 'opensuse15', 'opensuse', 'windows'
          }
          axis {
              name 'arch'
              values 'amd64'
          }
          axis {
              name 'flavor'
              values 'desktop', 'server', 'electron'
          }
                            }

        excludes {
          exclude {
            axis {
              name 'flavor'
              values 'electron'
                            }
            axis {
              name 'os'
              notValues 'bionic', 'windows'
            }
          }
          exclude {
            axis {
              name 'agent_label'
              values 'windows'
                                    }
            }
            axis {
              name 'os'
              notValues 'windows'
            }
          }
          exclude {
            axis {
              name 'agent_label'
              values 'linux'
            }
            axis {
              name 'os'
              values 'windows'
            }
          }
          exclude {
            axis {
              name 'os'
              values 'windows'
            }
            axis {
              name 'flavor'
              notValues 'server'
            }
          }
        }

        // Linux specific steps ===================================================================================================================================================
        stages {
                retry(2) {
          stage('Compile Package') {
            when {
              not { environment name: 'os', value: 'windows' }
            }

            agent {
              docker {
                image "jenkins/ide:${os}-${arch}-${env.BRANCH_NAME}"
                registryCredentialsId 'ecr:us-east-1:aws-build-role'
                registryUrl 'https://263245908434.dkr.ecr.us-east-1.amazonaws.com'
                reuseNode true
                  }
                }

            environment {
              CODESIGN_KEY = credentials('gpg-codesign-private-key')
              CODESIGN_PASS = credentials('gpg-codesign-passphrase')
              RSTUDIO_VERSION_MAJOR = '${rstudioVersionMajor}'
              RSTUDIO_VERSION_MINOR = '${rstudioVersionMinor}'
              RSTUDIO_VERSION_PATCH = '${rstudioVersionPatch}'
              RSTUDIO_VERSION_SUFFIX = '${rstudioVersionSuffix}'

              dir('package/linux') {
                sh "./make-${flavor}-package ${PACKAGE_TYPE} clean"
                sh "../../docker/jenkins/sign-release.sh /build-${flavor.capitalize()}-${PACKAGE_TYPE}/rstudio-*.${PACKAGE_TYPE.toLowerCase()} ${CODESIGN_KEY} ${CODESIGN_PASS}"
                  }
                }
                }

          stage('Run Tests') {
            when {
              not { environment name: 'os', value: 'windows' }
                  }

            agent {
              docker {
                image "jenkins/ide:${os}-${arch}-${env.BRANCH_NAME}"
                reuseNode true
                  }
                }
                                if (current_container.flavor == "electron") {
                                    stage('electron tests'){
                                        try {
                                            bat 'cd src/node/desktop && scripts\\run-unit-tests.cmd'
                                        }
                                        catch(err){
                                            currentBuild.result = "UNSTABLE"
                                        }
                                    }
                                }

            steps {
              dir("package/linux/${flavor.capitalize()}-${PACKAGE_TYPE}/src/gwt") {
                sh './gwt-unit-tests.sh'
              }
              dir("package/linux/${flavor.capitalize()}-${PACKAGE_TYPE}/src/cpp") {
                    bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17134.0\\x86\\signtool\" sign /f %pfx-file% /p %pfx-passphrase% /v /debug /n \"RStudio PBC\" /t http://timestamp.digicert.com  package\\win32\\build\\${packageName}.exe"
              }
                    bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17134.0\\x86\\signtool\" verify /v /pa package\\win32\\build\\${packageName}.exe"
                  }
                }

          stage('Upload Artifacts') {
            when {
              not { environment name: 'os', value: 'windows' }
            }

            agent {
              docker {
                image "jenkins/ide:${os}-${arch}-${env.BRANCH_NAME}"
                reuseNode true
                    }
                  }

            environment {
              SENTRY_API_KEY = credentials('ide-sentry-api-key')
              GITHUB_LOGIN = credentials('github-rstudio-jenkins')
              AWS_BUCKET="rstudio-ide-build"
              AWS_PATH="${flavor}/${os}/${PACKAGE_ARCH}/"
              PRODUCT="${flavor}"
              RSTUDIO_VERSION=" ${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}${rstudioVersionSuffix}"
                }

            steps {
              script {
                    if (rstudioVersionSuffix.contains("pro")) {
                  if (env.PRODUCT == "desktop") {
                    env.PRODUCT = "desktop-pro"
                  } else if (env.PRODUCT == "electron") {
                    env.PRODUCT = "electron-pro"
                  } else if (env.PRODUCT == "server") {
                    env.PRODUCT = "workbench"
                    }
                }

                def packageDir = "package/linux/build-${flavor.capitalize()}-${PACKAGE_TYPE}/"
                def tarballDir = "_CPack_Packages/Linux/${PACKAGE_TYPE}"
                def packageFile = ''
                def tarballFile = ''
                dir(packageDir) {
                  // Upload the pacakge to S3
                  env.packageFile = findFiles glob: 'rstudio-*.${PACKAGE_TYPE.toLowerCase()}'
                  // Strip relwithdebinfo froem the filename
                  def renamedFile = echo $packageFile | sed 's/-relwithdebinfo//'
                  mv $packageFile $renamedFile
                  packageFile=$renamedFile

                  withAWS(credentials: 'jenkins-aws') {
                    s3Upload acl: 'BucketOwnerFullControl', bucket: "$AWS_BUCKET", file: "$packageFile", path: "$AWS_PATH"
                  }

                  // Also upload installer-less version for desktop builds
                  if ((flavor == "desktop") || (flavor == "electron")) {
                    dir(tarballDir) {
                      tarballFile = findFiles glob: '*.tar.gz'
                      // Strip relwithdebinfo from the filename
                      def renamedTarball = echo $tarballFile | sed 's/-relwithdebinfo//'
                      mv $tarballFile $renamedTarball
                      tarballFile=$renamedTarball

                      withAWS(credentials: 'jenkins-aws') {
                        s3Upload acl: 'BucketOwnerFullControl', bucket: "$AWS_BUCKET", file: "$tarballFile", path: "$AWS_PATH"
                  }
                }
                                    stage('upload debug symbols') {
                  }

                  // Upload stripped debinfo to sentry
                  dir('src/cpp') {
                    retry 5 {
                      timeout activity: true, time: 15 {
                        sh "../../../../../docker/jenkins/sentry-upload.sh ${SENTRY_API_KEY}"
                    }
                  }
                }
              }

                // Publish to the dailies page
                dir('docker/jenkins') {
                  sh "./publish-build.sh --build ${PRODUCT}/${os} --url https://s3.amazonaws.com/rstudio-ide-build/${flavor}/${os}/${PACKAGE_ARCH}/${packageFile} --pat ${GITHUB_LOGIN_PSW} --file ${packageDir}/${packageFile} --version ${RSTUDIO_VERSION}"
                  if ((flavor == "desktop") || (flavor == "electron")) {
                    sh "./publish-build.sh --build ${PRODUCT}/${os} --url https://s3.amazonaws.com/rstudio-ide-build/${flavor}/${os}/${PACKAGE_ARCH}/${tarballFile} --pat ${GITHUB_LOGIN_PSW} --file ${packageDir}/${tarballDir}/${tarballFile} --version ${RSTUDIO_VERSION}"
            }
          }
        }
            }
        }
            }
          }
          // ========================================================================================================================================================================

          // Windows specific steps =================================================================================================================================================
          // stage('Compile Package') {
          //   when {
          //     environment name: 'os', value: 'windows'
          //   }

          //   agent {
          //     docker {
          //       image "jenkins/ide:${os}-${arch}-${env.BRANCH_NAME}"
          //       registryCredentialsId 'ecr:us-east-1:aws-build-role'
          //       registryUrl 'https://263245908434.dkr.ecr.us-east-1.amazonaws.com'
          //       reuseNode true
          //     }
          //   }

          //   environment {
          //     CODESIGN_KEY = credentials('ide-windows-signing-pfx')
          //     CODESIGN_PASS = credentials('ide-pfx-passphrase')
          //     RSTUDIO_VERSION_MAJOR = '${rstudioVersionMajor}'
          //     RSTUDIO_VERSION_MAJOR = '${rstudioVersionMajor}'
          //     RSTUDIO_VERSION_MAJOR = '${rstudioVersionMajor}'
          //     RSTUDIO_VERSION_MAJOR = '${rstudioVersionMajor}'
          //   }

          //   steps {
          //     script {
          //       def packageName =  "RStudio-${versionWithoutPlus}-RelWithDebInfo"
          //     }
              
          //     dir('package/win32') {
          //       bat "make-package.bat clean ${current_container.flavor}"

          //       dir('build') {
          //         bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17134.0\\x86\\signtool\" sign /f %CODESIGN_KEY% /p %CODESIGN_PASS% /v /debug /n \"RStudio PBC\" /t http://timestamp.digicert.com  ${packageName}.exe"
          //         bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17134.0\\x86\\signtool\" verify /v /pa ${packageName}.exe"
          //       }
          //     }
          //   }
          // }

          // stage('Run Tests') {
          //   when {
          //     environment name: 'os', value: 'windows'
          //   }

          //   docker {
          //     image "jenkins/ide:${os}-${arch}-${env.BRANCH_NAME}"
          //     reuseNode true
          //   }

          //   steps {
          //     bat 'rstudio-tests.bat --scope core'
          //   }
          // }

          // stage('Upload Artifacts') {
          //   when {
          //     environment name: 'os', value: 'windows'
          //   }

          //   docker {
          //     image "jenkins/ide:${os}-${arch}-${env.BRANCH_NAME}"
          //     reuseNode true
          //   }

          //   environment {
          //     CODESIGN_KEY = credentials('ide-windows-signing-pfx')
          //   }
          // }
          // ========================================================================================================================================================================
        }
      }
    }
  }
}