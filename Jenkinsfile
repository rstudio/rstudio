pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile.dispatcher'
      label 'linux && amd64'
    }
  }
  
  environment {
    COMMIT_HASH=""
  }

  stages {
    stage ("Define Variables") {
      steps {
        script {
          echo "Finding the commit."
          COMMIT_HASH = sh returnStdout: true, script: 'git rev-parse HEAD'
          echo "Commit = ${COMMIT_HASH}"
          build wait: false,
                job: "IDE/experiments/downstream-job-test/${env.BRANCH_NAME.replace('/', '%2F')}",
                parameters: [gitParameter(name: "COMMIT_HASH", value: env.COMMIT_HASH)]
        }
      }
    }

    stage ("Trigger Builds") {
      stages {
        stage ("Trigger Docs") {
          when {
            anyOf {
              changeset comparator: 'REGEXP', pattern: 'docs/.*'
              changeset comparator: 'REGEXP', pattern: 'version/.*'
            }
          }

          steps {
            echo "Would trigger doc builds at ${COMMIT_HASH}"
          }
        }

        stage ("Trigger Binary Builds") {
          when {
            allOf {
              changeset comparator: 'REGEXP', pattern: '(?!docs/).+'
              not {
                changeset 'Jenkinsfile'
              }
            }
          }

          steps {
            echo "Would trigger binary builds at ${COMMIT_HASH}"
          }

        }
      }
    }
  }
}
