#!groovy
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def props = [:]

def runjenkinsfile(){

    node {
        timestamps {
            echo "Inside Jenkinsfile for Apigee"
            cleanWs notFailBuild: true
            def yamlPath = "${pwd()}/project.yaml"

            //checkout the project
            checkout scm

            echo "${yamlPath} used for this execution"
            sh("cat ${yamlPath}")

            // read project.yaml to setup def variable and tools
            props = readYaml file: 'project.yaml'
            projectName = props.build.name
            buildType = props.build.type
            def buildVersion = props.build.buildVersion
            def apiversion = "${buildVersion}.${env.BUILD_NUMBER}"
            def workspaceDirectory = "${env.WORKSPACE}"
            def jobName = "${env.JOB_NAME}"
            def gitBranch = "${env.BRANCH_NAME}"
            def gitRepo = env.JOB_NAME.split('/')[0]
            def proxyName = env.JOB_NAME.split('/')[0].split('-')[1]
            def mavenHome = tool name: "Maven339", type: 'maven'

            // Start Jenkins Build Process
            try {
                // Stage build
                stage('Build') {

                    // echo out the projects: GitHub Project, it repo and branch
                    echo("workspaceDirectory = $workspaceDirectory")
                    echo("jobName = $jobName")
                    echo("gitBranch = $gitBranch")
                    echo("gitRepo = $gitRepo")
                    echo("apiversion = $apiversion")

                    // echo out the projects: Build number, Build Version and Apiversion Number
                    echo "Build Number is ${env.BUILD_NUMBER}"
                    echo "Build Version is $buildVersion"
                    echo "Apiversion Number is {$apiversion}"

                    // call the script bundle.sh which will build the proxy/sharedflow files and upload them to artifactory. Note artifactory url is set in the script.
                    sh("./bundle.sh ${props.build.proxy} ${workspaceDirectory} ${proxyName} ${buildType}")
                }

            }
            catch (e) {
                //If there was an exception thrown, the build failed
                currentBuild.result = "FAILED"
                throw e
            }
        }
    } //END NODE
}
return this;
