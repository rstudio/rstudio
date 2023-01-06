def BINARY_JOB_ROOT="IDE/OS-Builds/Nightly"

pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile.dispatcher'
      label 'linux && amd64'
    }
  }
  
  environment {
    COMMIT_HASH=""
    BUILD_BRANCH="${env.BRANCH_NAME.replace('/', '%2F')}"
  }

  stages {
    stage ("Define Variables") {
      steps {
        script {
          echo "Finding the commit."
          COMMIT_HASH = sh returnStdout: true, script: 'git rev-parse HEAD'
          echo "Commit = ${COMMIT_HASH}"
        }
      }
    }

    stage ("Trigger Builds") {
      stages {
        stage ("Trigger Open Source Builds") {
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
