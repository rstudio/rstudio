def commit_hash

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
        }
      }
    }

    stage ("Trigger Builds") {
      stages {
        stage ("Trigger Docs") {
          when {
            anyOf {
              changeset 'docs/*'
              changeset 'version/*'
            }
          }

          steps {
            echo "Would trigger doc builds at ${COMMIT_HASH}"
          }
        }

        stage ("Trigger Binary Builds") {
          when {
            changeset comparator: 'REGEXP', pattern: '(?!docs/).+'
          }

          steps {
            echo "Would trigger binary builds at ${COMMIT_HASH}"
          }

        }
      }
    }
  }
}