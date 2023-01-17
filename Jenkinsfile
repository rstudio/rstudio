def BINARY_JOB_ROOT="IDE/OS-Builds/Nightly"
def utils

pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile.dispatcher'
      label 'linux && amd64'
    }
  }
  
  environment {
    RSTUDIO_VERSION = ""
    RSTUDIO_VERSION_MAJOR = 0
    RSTUDIO_VERSION_MINOR = 0
    RSTUDIO_VERSION_PATCH = 0
    RSTUDIO_VERSION_SUFFIX = 0
    COMMIT_HASH=""
    BUILD_BRANCH="${env.BRANCH_NAME.replace('/', '%2F')}"
  }

  stages {
    stage ("Set Version & Commit") {
      steps {
        script {
          utils = load "${env.WORKSPACE}/utils.groovy"
          
          // Get the current commit
          COMMIT_HASH = sh returnStdout: true, script: 'git rev-parse HEAD'

          // Get the version
          (RSTUDIO_VERSION,
            RSTUDIO_VERSION_MAJOR,
            RSTUDIO_VERSION_MINOR,
            RSTUDIO_VERSION_PATCH,
            RSTUDIO_VERSION_SUFFIX) = utils.getVersion()
        }
      }
    }

    stage ("Trigger Builds") {
      stages {
        stage ("Open Source Builds") {
          when {
            allOf {
              changeset comparator: 'REGEXP', pattern: '(?!docs/).+'
              not {
                changeset 'Jenkinsfile'
              }
            }
          }

          steps {
            echo "Would trigger binary builds at ${COMMIT_HASH} on branch ${env.BUILD_BRANCH}"

            build wait: false,
                  job: "${BINARY_JOB_ROOT}/Windows/${env.BUILD_BRANCH}",
                  parameters: [gitParameter(name: "COMMIT_HASH", value: "${COMMIT_HASH}")]

            build wait: false,
                  job: "${BINARY_JOB_ROOT}/Linux/${env.BUILD_BRANCH}",
                  parameters: [gitParameter(name: "COMMIT_HASH", value: "${COMMIT_HASH}")]

            build wait: false,
                  job: "${BINARY_JOB_ROOT}/MacOS/${env.BUILD_BRANCH}",
                  parameters: [gitParameter(name: "COMMIT_HASH", value: "${COMMIT_HASH}")]
          }
        }
      }
    }
  }
}
