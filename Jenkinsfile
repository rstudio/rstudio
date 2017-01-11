#!groovy

properties([
    buildDiscarder(logRotator(artifactDaysToKeepStr: '',
                              artifactNumToKeepStr: '10',
                              daysToKeepStr: '',
                              numToKeepStr: '20'))
])

def resolve_deps(type) {
  switch ( type ) {
      case "DEB":
        sh 'cd dependencies/linux && ./install-dependencies-debian && cd ../..'
        break
      case "RPM":
        sh 'cd dependencies/linux && ./install-dependencies-yum && cd ../..'
        break
  }
}

def compile_package_and_archive(type, flavor) {
  sh "cd package/linux && ./make-${flavor}-package ${type} clean && cd ../.."
  archiveArtifacts artifacts: 'package/linux/build-${flavor.capitalize()}-${type}/rstudio-*.${type.toLowerCase()}'
}

def s3_upload(type, flavor) {
  sh "aws s3 mv package/linux/build-${flavor.capitalize()}-${type}/rstudio-*.${type.toLowerCase()} s3://rstudio-devops/opsworks/"
}

def gitClean() {
 // rstudio/connect
 sh 'git reset --hard'
 sh 'git clean -ffdx'
}

def prepareWorkspace(){
  step([$class: 'WsCleanup'])
  checkout scm
  gitClean() 
}

try {
    timestamps {
        def containers = [
          [type: 'DEB', image: 'precise_64', flavor: 'desktop'],
          [type: 'DEB', image: 'precise_32', flavor: 'desktop'],
          [type: 'DEB', image: 'precise_64', flavor: 'server'],
          [type: 'DEB', image: 'precise_32', flavor: 'server'],
          [type: 'RPM', image: 'centos6_64', flavor: 'server'],
          [type: 'RPM', image: 'centos6_32', flavor: 'server'],
          [type: 'RPM', image: 'centos5_64', flavor: 'server'],
          [type: 'RPM', image: 'centos5_32', flavor: 'server'],
          [type: 'RPM', image: 'centos7_64', flavor: 'desktop'],
          [type: 'RPM', image: 'centos7_32', flavor: 'desktop']

        ]
        def parallel_containers = [:]
        for (int i = 0; i < containers.size(); i++) {
            def index = i
            parallel_containers["${containers[i].type}-${containers[i].image}-${containers[i].flavor}"] = {
                def current_container = containers[index]
                node('ide') {
                    prepareWorkspace() // empties the existing workspace and checks out current commit.
                    container = docker.build("rstudio-ide-${current_container.image}", "-f docker/jenkins/Dockerfile.${current_container.image} .")
                    container.inside("-u 0:0") {
                        stage('resolve deps'){
                            resolve_deps(current_container.type)
                        }
                        stage('compile package') {
                            compile_package_and_archive(current_container.type, current_container.flavor)
                        }
                    }
                    stage('upload artifacts') {
                        s3_upload(current_container.type, current_container.flavor)
                    }
                }
            }
        }
        parallel parallel_containers
        slackSend channel: '@steve', color: 'good', message: "RStudio ide build finished successfully."
    }
  
} catch(err) {
   slackSend channel: '@steve', color: 'bad', message: "failed: ${err}"
   error("failed: ${err}")
}
