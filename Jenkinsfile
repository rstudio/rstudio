def commit_hash

pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile.dispatcher'
      label 'linux && amd64'
    }
  }

  stages {
    stage ("Define Variables") {
      steps {
        script {
          sh 'echo Finding the commit.'
          commit_hash = sh returnStdout: true, script: 'git rev-parse HEAD'
          sh 'echo Commit = ${commit_hash}'
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