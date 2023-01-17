@Library('pipeline-shared-libraries@feature/windows-pbp') _
def utils

pipeline {

  agent none

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(
      logRotator(
        artifactDaysToKeepStr: '',
        artifactNumToKeepStr: '',
        daysToKeepStr: '',
        numToKeepStr: '100'
      )
    )
  }

  parameters {
    string(name: 'SLACK_CHANNEL', defaultValue: '#ide-builds', description: 'Slack channel to publish build message.')
    string(name: 'RSTUDIO_VERSION_PATCH',  defaultValue: '999', description: 'RStudio Patch Version')
    booleanParam(name: 'RSTUDIO_SKIP_QT', defaultValue: false, description: 'Skips installing and bulding for QT')
    booleanParam(name: 'DAILY', defaultValue: false, description: 'Runs daily build if true')
    booleanParam(name: 'PUBLISH', defaultValue: true, description: 'Runs publish stage if true')
    gitParameter defaultValue: "${env.GIT_BRANCH}", name: 'COMMIT_HASH', type: 'PT_REVISION'
  }

  environment {
    PACKAGE_OS = 'Windows'
    AWS_ACCOUNT_ID = '749683154838'
  }

  stages {

    stage('Load Utils') {
      steps {
        script {
          sh 'printenv'
          sh "echo 'Loading utils from ${env.WORKSPACE}/utils.groovy'"
          utils = load "${env.WORKSPACE}/utils.groovy"
        }
      }
    }

    stage ("Checkout") {
      steps {
        echo "Commit_hash value: ${params.COMMIT_HASH}"
          checkout([$class: 'GitSCM',
            branches: [[name: "${params.COMMIT_HASH}"]],
            extensions: [],
            userRemoteConfigs: [[credentialsId: 'github-rstudio-jenkins', url: 'https://github.com/rstudio/rstudio']]])
      }
    }

    stage('Versioning') {
      steps {
        script {
          (RSTUDIO_VERSION,
            RSTUDIO_VERSION_MAJOR,
            RSTUDIO_VERSION_MINOR,
            RSTUDIO_VERSION_PATCH,
            RSTUDIO_VERSION_SUFFIX) = utils.getVersion()
          RSTUDIO_RELEASE = "${RSTUDIO_VERSION_MAJOR}.${RSTUDIO_VERSION_MINOR}.${RSTUDIO_VERSION_PATCH}${RSTUDIO_VERSION_SUFFIX}"
          RSTUDIO_FLOWER = readFile(file: 'version/RELEASE').replaceAll(" ", "-").toLowerCase().trim()
          currentBuild.displayName = "${RSTUDIO_RELEASE}"
          IS_PRO = RSTUDIO_VERSION_SUFFIX.contains('pro')
        }
      }
    }

    stage('Prepare Windows Container') {
      steps {
        withAWS(role: 'build', roleAccount: AWS_ACCOUNT_ID) {
          pullBuildPush(
            image_name: 'jenkins/ide',
            dockerfile: "docker/jenkins/Dockerfile.windows",
            image_tag: "windows-${RSTUDIO_FLOWER}",
            build_args: utils.jenkins_user_build_args(),
            build_arg_jenkins_uid: null, // Ensure linux-only step is not run on windows (id -u jenkins)
            build_arg_jenkins_gid: null, // Ensure linux-only step is not run on windows (id -g jenkins)
            build_arg_docker_gid: null, // Ensure linux-only step is not run on windows (stat -c %g /var/run/docker.sock)
            retry_image_pull: 5)
        }
      }
    }

    stage ('Build Matrix') {

      matrix {

        axes {
          axis {
            name 'FLAVOR'
              values 'Electron', 'Desktop' // desktop denotes a Qt build
          }
        }

        when {
          anyOf {
            environment name: 'FLAVOR', value: 'Electron'
            expression { return FLAVOR == 'Desktop' && IS_PRO == true } // Only build Qt on Pro
          }
        }

        stages {
          stage('Build Windows') {
            agent {
              docker {
                image "jenkins/ide-docs:${env.BRANCH_NAME.replaceAll('/', '-')}"
                  registryUrl 'https://263245908434.dkr.ecr.us-east-1.amazonaws.com'
                  registryCredentialsId 'ecr:us-east-1:aws-build-role'
                  reuseNode true
              }
            }

            stages { 
              stage('Build') {

                environment {
                  CODESIGN_KEY = credentials('ide-windows-signing-pfx')
                  CODESIGN_PASS = credentials('ide-pfx-passphrase')
                }

                steps {
                  // set requisite environment variables and build rstudio
                  bat '''
                    cd package/win32 &&
                    set "rstudio_version_major=${RSTUDIO_VERSION_MAJOR}" &&
                    set "rstudio_version_minor=${RSTUDIO_VERSION_MINOR}" && 
                    set "rstudio_version_patch=${RSTUDIO_VERSION_PATCH}" && 
                    set "rstudio_version_suffix=${RSTUDIO_VERSION_SUFFIX}" &&
                    set "package_os=windows" && 
                    make-package.bat clean ${FLAVOR} && 
                    cd ../..
                  '''
                }
              }

              stage('Tests') {
                steps {
                  bat 'cd package/win32/build/src/cpp && rstudio-tests.bat --scope core'
                }
              }

              stage('Electron Tests') {
                when { environment name: 'FLAVOR', value: 'Electron' }
                steps {
                  bat 'cd src/node/desktop && scripts\\run-unit-tests.cmd'
                }
              }

              stage('Sign, Upload, and Publish') {
                when { expression { return params.PUBLISH } }

                stages {
                  stage('Sign') {
                    steps {
                      script {
                        packageVersion = "${RSTUDIO_RELEASE}".replace('+', '-')

                        def packageName = "RStudio-${packageVersion}-RelWithDebInfo"

                        withCredentials([file(credentialsId: 'ide-windows-signing-pfx', variable: 'pfx-file'), string(credentialsId: 'ide-pfx-passphrase', variable: 'pfx-passphrase')]) {
                          bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.19041.0\\x86\\signtool\" sign /f %pfx-file% /p %pfx-passphrase% /v /debug /n \"RStudio PBC\" /t http://timestamp.digicert.com  package\\win32\\build\\${packageName}.exe"

                            bat "\"C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.19041.0\\x86\\signtool\" verify /v /pa package\\win32\\build\\${packageName}.exe"
                        }
                      }
                    }
                  }

                  stage('Upload') {
                    steps {
                      script {
                        packageVersion = "${RSTUDIO_RELEASE}".replace('+', '-')

                        def buildDest = "s3://rstudio-ide-build/${current_container.flavor}/windows"
                        def packageName = "RStudio-${packageVersion}"

                        // strip unhelpful suffixes from filenames
                        bat "move package\\win32\\build\\${packageName}-RelWithDebInfo.exe package\\win32\\build\\${packageName}.exe"
                        bat "move package\\win32\\build\\${packageName}-RelWithDebInfo.zip package\\win32\\build\\${packageName}.zip"

                        // windows docker container cannot reach instance-metadata endpoint. supply credentials at upload.
                        withAWS(role: 'jenkins', roleAccount: AWS_ACCOUNT_ID) {
                          retry(5) {
                            bat "aws s3 cp package\\win32\\build\\${packageName}.exe ${buildDest}/${packageName}.exe"
                            bat "aws s3 cp package\\win32\\build\\${packageName}.zip ${buildDest}/${packageName}.zip"
                          }
                        }
                      }
                    }
                  }

                  stage('Sentry Upload') {
                    environment {
                      SENTRY_API_KEY = credentials('ide-sentry-api-key')
                    }

                    steps {
                      retry(5) {
                        // convert the PDB symbols to breakpad format (PDB not supported by Sentry)
                        timeout(activity: true, time: 15) {
                          bat '''
                            cd package\\win32\\build
                            FOR /F %%G IN ('dir /s /b *.pdb') DO (..\\..\\..\\dependencies\\windows\\breakpad-tools-windows\\dump_syms %%G > %%G.sym)
                          '''

                          // upload the breakpad symbols
                          withCredentials([string(credentialsId: 'ide-sentry-api-key', variable: 'SENTRY_API_KEY')]) {
                            // attempt to run sentry uplaod
                            bat "cd package\\win32\\build\\src\\cpp && ..\\..\\..\\..\\..\\dependencies\\windows\\sentry-cli.exe --auth-token %SENTRY_API_KEY% upload-dif --log-level=debug --org rstudio --project ide-backend -t breakpad ."
                          }
                        }
                      }
                    }
                  }

                  stage ('Publish') {
                    steps {
                      script {
                        def packageVersion = "${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}${rstudioVersionSuffix}"
                        packageVersion = packageVersion.replace('+', '-')

                        def packageName = "RStudio-${packageVersion}"
                        withCredentials([usernamePassword(credentialsId: 'github-rstudio-jenkins', usernameVariable: 'GITHUB_USERNAME', passwordVariable: 'GITHUB_PAT')]) {

                          // derive product
                          def product = "${current_container.flavor}"
                            if (rstudioVersionSuffix.contains("pro")) {
                              product = "${current_container.flavor}-pro"
                            }

                          // publish the build (self installing exe)
                          def stdout = powershell(returnStdout: true, script: ".\\docker\\jenkins\\publish-build.ps1 -build ${product}/windows -url https://s3.amazonaws.com/rstudio-ide-build/${current_container.flavor}/windows/${packageName}.exe -pat ${GITHUB_PAT} -file package\\win32\\build\\${packageName}.exe -version ${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}${rstudioVersionSuffix}")
                            println stdout

                          // publish the build (installer-less zip)
                          stdout = powershell(returnStdout: true, script: ".\\docker\\jenkins\\publish-build.ps1 -build ${product}/windows-xcopy -url https://s3.amazonaws.com/rstudio-ide-build/${current_container.flavor}/windows/${packageName}.zip -pat ${GITHUB_PAT} -file package\\win32\\build\\${packageName}.zip -version ${rstudioVersionMajor}.${rstudioVersionMinor}.${rstudioVersionPatch}${rstudioVersionSuffix}")
                          println stdout
                        }
                      }
                    }
                  }
                }
              }

            }
          }
        }
      }
    }
  }

  post {
    always {
      deleteDir()
        sendNotifications slack_channel: SLACK_CHANNEL
    }
  }

}
