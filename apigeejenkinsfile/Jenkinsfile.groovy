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
   def gitCloneURL = 'https://github.com/Darshan-shivashankar/jenkins-apigee-build.git'
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
      git branch: 'master', credentialsId: gitCred, poll: false, url: gitCloneURL
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

deployProxy(props)

}
return this;

def deployProxy(props) {
 def buildNumber = env.BUILD_NUMBER
 def chkInBranch = "${env.BRANCH_NAME}"
 timestamps {
  // read project.yaml to setup def variables and tools
  // function to get list of Deploy Environments
  def allDeployEnvs = props.deploy
  def bldver = props.build.version
  def apiversion = "Jenkins Build Number: ${proxyName}-${version}-${env.BUILD_NUMBER}"
  echo "allDeployEnvs = ${allDeployEnvs}"
  try {
   // For loop to go through Environments to Deploy
   for (def env: allDeployEnvs) {
    if (env.branch == chkInBranch) {
     // Stage for each Environments to be Deploy too
     stage("Deploy to ${env.env}") {
      echo "Test"
     }
    }

   }

 } catch (e) {
  //If there was an exception thrown, the Deploy failed
  currentBuild.result = "FAILED"
  notifyFailed(email)
  throw e
 }

}

}
