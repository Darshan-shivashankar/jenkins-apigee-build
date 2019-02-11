#!groovy

import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def props = [: ]

def runjenkinsfile() {

  node {
    timestamps {
      cleanWs notFailBuild: true
      def yamlPath = "${pwd()}/project.yaml"
      //checkout the project
      checkout scm
      echo "${yamlPath} used for this execution"
      sh("cat ${yamlPath}")

      // read project.yaml to setup def variable and tools
      props = readYaml file: 'project.yaml'
      proxyName = props.build.name
      buildType = props.build.type
      def gitURL = 'https://github.com/Darshan-shivashankar'
      def gitCred = 'cdc64b8b-f85a-4a29-8b36-6926d19f7473'
      def gitBuildURL = 'https://github.com/Darshan-shivashankar/jenkins-apigee-build.git'
      def version = props.build.version
      def apiversion = "Jenkins Build Number: ${proxyName}-${version}-${env.BUILD_NUMBER}"
      def workspaceDirectory = "${env.WORKSPACE}"
      def jobName = "${env.JOB_NAME}"
      def gitBranch = "${env.BRANCH_NAME}"
      def gitRepo = env.JOB_NAME.split('/')[0]
      def projectName = env.JOB_NAME.split('/')[0].split('-')[1]
      def artifactoryURLForSharedFlow = 'http://demo.itorix.com:8081/artifactory/apigee-sharedflow-build'
      def artifactoryURLForProxy = 'http://demo.itorix.com:8081/artifactory/apigee-proxy-build'
      def artifactoryCred = '55152e81-eebf-4ce8-83a2-ef6b5f7666d5'
      def mavenHome = tool name: "Maven339", type: 'maven'

      // Start Jenkins Build Process
      try {
        // Stage Build
        stage('Build') {

          // echo out the projects: GitHub Project, it repo and branch
          echo("workspaceDirectory = $workspaceDirectory")
          echo("jobName = $jobName")
          echo("gitBranch = $gitBranch")
          echo("gitRepo = $gitRepo")
          echo("apiversion = $apiversion")
          echo("buildType = $buildType")

          // echo out the projects: Build number, Build Version and Apiversion Number
          echo "Build Number is ${env.BUILD_NUMBER}"
          echo "Build Version is $version"
          echo "Apiversion Number is $apiversion"

          dir('Build') {
            git branch: 'master', credentialsId: gitCred, poll: false, url: gitBuildURL
          }
          dir(gitRepo) {
            git branch: gitBranch, credentialsId: gitCred, poll: false, url: gitURL + '/' + gitRepo + '.git'
          }

          // call the script bundle.sh which will build the proxy/sharedflow files

          sh("ln -s ${gitRepo} ${proxyName};chmod a+x Build/bundle.sh;cd Build;./bundle.sh ${workspaceDirectory} ${projectName} ${buildType}")
          //sh("ln -s ${gitRepo} ${proxyName};chmod 0755 -R *;cd Build;./bundle.sh ${workspaceDirectory} ${projectName} ${buildType}")

        }

        // Stage Upload to Artifactory
        stage('Upload') {

          def artifactoryNumber = "${env.BUILD_NUMBER}"

          // echo out the projects: GitHub Project, it repo and branch
          echo("workspaceDirectory = $workspaceDirectory")
          echo("jobName = $jobName")
          echo("gitBranch = $gitBranch")
          echo("gitRepo = $gitRepo")
          echo("apiversion = $apiversion")
          echo("buildType = $buildType")

          // echo out the projects: Build number, Build Version and Apiversion Number
          echo "Build Number is ${env.BUILD_NUMBER}"
          echo "Build Version is $version"
          echo "Apiversion Number is $apiversion"

          //Publish to artifactory

          withCredentials([usernamePassword(credentialsId: artifactoryCred, passwordVariable: 'password', usernameVariable: 'username')]) {
            sh("ln -s ${gitRepo} ${proxyName};chmod a+x Build/artifactory.sh;cd Build;./artifactory.sh ${version} ${workspaceDirectory} ${projectName} ${buildType} ${artifactoryNumber} ${artifactoryURLForSharedFlow} ${artifactoryURLForProxy} ${username} ${password}")
            //sh("ln -s ${gitRepo} ${proxyName};chmod 0755 -R *;cd Build;./artifactory.sh ${workspaceDirectory} ${projectName} ${buildType} ${apiversion} ${artifactoryURLForSharedFlow} ${artifactoryURLForProxy} ${username} ${password} ${version}")
          }
        }

        } catch (e) {
          //If there was an exception thrown, the build failed
          currentBuild.result = "FAILED"
          throw e
        }
      }
      } //END NODE

      // If deployproxy is set to true in the project.yaml file we will run the deployProject function
      if (props.deployProxy) {
        deployProxy(props)
      }

    }
    return this;

    def deployProxy(props) {
      def gitBranch = "${env.BRANCH_NAME}"
      echo "${gitBranch}"
      timestamps {
        def allDeployEnvs = props.deploy
        def bldver = props.build.version
        echo "allDeployEnvs = ${allDeployEnvs}"
        try {
          // For loop to go through Environments to Deploy
          for (def deployenv: allDeployEnvs) {
            echo "BRANCH_NAME ${deployenv.branch}"
            if (deployenv.branch == gitBranch) {
              echo "${deployenv}"
              stage("Deploy to ${deployenv.env}") {
                echo "Deploying Proxy and Sharedflow Process Started"
                def apigeeEnv = "${deployenv.env}"
                def approval = "${deployenv.approval}"


                if (apigeeEnv == null ) {
                    echo 'Cannot deploy without apigee environment defined under project.yaml: env type define example type: Dev, type: QA, type: Prod '
                    currentBuild.rawBuild.result = Result.ABORTED
                    throw new hudson.AbortException('Cannot deploy without ENV type define example type: Dev, type: QA, type: Prod ')
                }
                //approval flow
                if (deployenv.approval) {
                  echo "Ok to deploy to ${deployenv.env}"
                  Approval(apigeeEnv)
                }
                // Launch node
                node {
                  proxyName = props.build.name
                  buildType = props.build.type

                  def gitCred = 'cdc64b8b-f85a-4a29-8b36-6926d19f7473'
                  def gitBuildURL = 'https://github.com/Darshan-shivashankar/jenkins-apigee-build.git'
                  def version = props.build.version
                  def apiversion = "Jenkins Build Number: ${proxyName}-${version}-${env.BUILD_NUMBER}"
                  def projectName = env.JOB_NAME.split('/')[0].split('-')[1]
                  def artifactoryCred = '55152e81-eebf-4ce8-83a2-ef6b5f7666d5'
                  def artifactoryNumber = "${env.BUILD_NUMBER}"
                  def apigeeCred = '6d127345-1cb4-4fdc-aa44-6d15bb096cc3'
                  def mavenHome = tool name: "Maven339", type: 'maven'
                  def workspaceDirectory = "${env.WORKSPACE}"
                  def artifactoryURLForSharedFlow = 'http://demo.itorix.com:8081/artifactory/apigee-sharedflow-build'
                  def artifactoryURLForProxy = 'http://demo.itorix.com:8081/artifactory/apigee-proxy-build'

                  //Apigee Related
                  def apigeeOrg = "${deployenv.org}"
                  def apigeeHost = "${deployenv.host}"
                  def apigeeEnvironment = "${deployenv.env}"
                  def apigeeProxy = projectName

                    // call the script deploy-pipe.sh which will download from artifactory and deploy the proxy files to the Apigee Host. Note artifactory URL and Host URL is set in the script.
                    withCredentials([
                      usernamePassword(credentialsId: artifactoryCred, passwordVariable: 'password', usernameVariable: 'username')
                      usernamePassword(credentialsId: apigeeCred, passwordVariable: 'apigeepassword', usernameVariable: 'apigeeusername')
                      ]){
                      sh("chmod a+x Build/deploy-pipe.sh;cd Build;./deploy-pipe.sh ${apigeeHost} ${apigeeOrg} ${apigeeEnvironment} ${apigeeProxy} ${version} ${username} ${password} ${artifactoryNumber} ${workspaceDirectory} ${buildType} ${artifactoryURLForSharedFlow} ${artifactoryURLForProxy} ${apigeeusername} ${apigeepassword}")
                    }
                }
              }
            }

          }
          } catch (e) {
            //If there was an exception thrown, the Deploy failed
            currentBuild.result = "FAILED"
            throw e
          }

        }

      }

      def Approval(apigeeEnv) {
          try {
              timeout(time: 720, unit: 'MINUTES') {
                input message: "Press deploy to proceed or Abort to stop the deployment to ${apigeeEnv} . ", ok: 'Deploy'
              }
          }
          catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException err) {
              ///If the Jod was ABORTED, send notify email and input for reason why pipeline was aborted
              currentBuild.result = "ABORTED"
              echo "Deploy timed out waiting for user approval"
              throw err
          }
      }
