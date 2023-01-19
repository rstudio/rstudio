def BINARY_JOB_ROOT="IDE/OS-Builds/Platforms"
def utils

def buildJob(String platform) {
  build wait: false,
        job: "${BINARY_JOB_ROOT}/${platform}/${env.BUILD_BRANCH}",
        parameters: [
          gitParameter(name: "COMMIT_HASH", value: "${COMMIT_HASH}"),
          string(name: "RSTUDIO_VERSION_PATCH", value: "${RSTUDIO_VERSION_PATCH}"),
          string(name: "SLACK_CHANNEL", value: "${SLACK_CHANNEL}"),
          booleanParam(name: "DAILY", value: true),
          booleanParam(name: "PUBLISH", value: env.PUBLISH)
}

pipeline {
  agent {
    dockerfile {
      filename 'Dockerfile.dispatcher'
      label 'linux && amd64'
    }
  }
  
  parameters {
    string defaultValue: '0', description: 'RStudio Patch Version', name: 'RSTUDIO_VERSION_PATCH', trim: true
    string defaultValue: '#ide-builds', description: 'Slack channel to publish build message.', name: 'SLACK_CHANNEL', trim: true
  }
  
  environment {
    RSTUDIO_VERSION = ""
    RSTUDIO_VERSION_MAJOR = 0
    RSTUDIO_VERSION_MINOR = 0
    RSTUDIO_VERSION_PATCH = 0
    RSTUDIO_VERSION_SUFFIX = 0
    COMMIT_HASH=""
    PUBLISH=false
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

        buildName "${RSTUDIO_VERSION}"
      }
    }

    stage ("Create a Sentry Release") {
      environment {
        SENTRY_API_KEY = credentials('ide-sentry-api-key')
      }

      when {
        branch comparator: 'REGEXP', pattern: 'main|elsbeth-geranium|ghost-orchid|cherry-blossom|mountain-hydrangea'
      }

      steps { 
        echo "Creating a sentry release for version ${RSTUDIO_VERSION}"

        // Install sentry
        sh "HOME=`pwd` ./dependencies/common/install-sentry-cli"

        // create new release on Sentry
        sh 'sentry-cli --auth-token ${SENTRY_API_KEY} releases --org rstudio --project ide-backend new ${RSTUDIO_VERSION}'

        // associate commits
        sh 'sentry-cli --auth-token ${SENTRY_API_KEY} releases --org rstudio --project ide-backend set-commits --auto ${RSTUDIO_VERSION}'

        // finalize release
        sh 'sentry-cli --auth-token ${SENTRY_API_KEY} releases --org rstudio --project ide-backend finalize ${RSTUDIO_VERSION}'

        script {
          PUBLISH=true
        }
      }
    }

    stage ("Trigger Builds") {
      parallel {
        stage ("Binary Builds") {
          when {
            allOf {
              changeset comparator: 'REGEXP', pattern: '(?!docs/).+'
              not {
                changeset 'Jenkinsfile'
              }
            }
          }

          steps {
            buildJob 'Windows'
            buildJob 'Linux'
            buildJob 'MacOS'
        }
      }
    }
  }

  post {
    always {
      sendNotifications slack_channel: SLACK_CHANNEL
    }
  }
}
