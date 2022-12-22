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
          sh 'echo Finding the commit.'
          COMMIT_HASH = sh returnStdout: true, script: 'git rev-parse HEAD'
          sh 'echo Commit = ${COMMIT_HASH}'
        }
      }
    }

    stage ("Trigger Builds") {
      stages {
        stage ("Trigger Docs") {
          // when {
          //   anyOf {
          //     changeset 'docs/*'
          //     changeset 'version/*'
          //   }
          // }

          steps {
            sh 'echo Would trigger doc builds at ${COMMIT_HASH}'
          }
        }

        stage ("Trigger Binary Builds") {
          // when {
          //   changeset comparator: 'REGEXP', pattern: '(?!docs/).*'
          // }

          steps {
            sh 'echo Would trigger binary builds at ${COMMIT_HASH}'
          }

        }
      }
    }
  }
}