pipeline {
  agent none

  stages {
    stage ("Define Variables") {
      steps {
        script {
          def commit_hash = sh git rev-parse HEAD
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
            sh 'echo Would trigger doc builds at ${commit_hash}'
          }
        }

        stage ("Trigger Binary Builds") {
          when {
            changeset comparator: 'REGEXP', pattern: '(?!docs/).*'
          }

          steps {
            sh 'echo Would trigger binary builds at ${commit_hash}'
          }

        }
      }
    }
  }
}