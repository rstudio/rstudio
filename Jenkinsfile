pipeline {
  agent { none }

  stages {
    stage ("Define Variables") {
      steps {
        step ("Set Commit Hash") {
          script {
            def commit_hash = sh git rev-parse HEAD
          }
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
            step ("Start doc build at commit") {
              sh 'echo Would trigger doc builds at ${commit_hash}'
            }
          }
        }

        stage ("Trigger Base Builds") {
          when {
            changeset comparator: 'REGEXP', pattern: '(?!docs/).*'
          }

          steps {
            step ("Start binary build at commit") {
              sh 'echo Would trigger binary builds at ${commit_hash}'
            }
          }

        }
      }
    }
  }
}