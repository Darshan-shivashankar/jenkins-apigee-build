#!groovy
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def props = [:]
def email = ''

def runjenkinsfile(){

    node('apigee-build') {
        timestamps {
            cleanWs notFailBuild: true
            def yamlPath = "${pwd()}/project.yaml"

            //checkout the project
            checkout scm

            echo "${yamlPath} used for this execution"
            sh("cat ${yamlPath}")

            // read project.yaml to setup def variable and tools
            props = readYaml file: 'project.yaml'
            email = props.mail
            def buildVersion = props.build.buildVersion
            def apiversion = "${buildVersion}.${env.BUILD_NUMBER}"
            def workspaceDirectory = "${env.WORKSPACE}"
            def jobName = "${env.JOB_NAME}"
            def gitBranch = "${env.BRANCH_NAME}"
            def gitRepo = env.JOB_NAME.split('/')[1]
            def mavenHome = tool name: "Maven339", type: 'maven'
            def curlHome = tool 'curl'

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

                    // Apigee API Build tools for Kaiser Pipeline. All Apigee API application will use this build repo "aabt" and need to be checkout
                    dir('Build') {
                        git branch: 'master', credentialsId: bitBucketCreds, poll: false, url: 'https://bitbucket.service.edp.t-mobile.com/scm/aabt/build.git'
                    }
                    // For each Apigee API Application project has the following repo's that need to get checkout Apigee API APP repo, common and target.
                    dir('Common') {
                        git branch: 'master', credentialsId: bitBucketCreds, poll: false, url: bitBucketURL + '/scm/' + bitBucketProject + '/' + 'Common.git'
                    }
                    dir('Targets') {
                        git branch: 'master', credentialsId: bitBucketCreds, poll: false, url: bitBucketURL + '/scm/' + bitBucketProject + '/' + 'Targets.git'
                    }
                    dir(projectBitBucketRepo) {
                        git branch: gitBranch, credentialsId: bitBucketCreds, poll: false, url: bitBucketURL + '/scm/' + bitBucketProject + '/' + projectBitBucketRepo + '.git'
                    }

                    // echo out the projects: Build number, Build Version and Apiversion Number
                    echo "Build Number is ${env.BUILD_NUMBER}"
                    echo "Build Version is $buildVersion"
                    echo "Apiversion Number is {$apiversion}"

                    // call the script bundle.sh which will build the proxy files and upload them to artifactory. Note artifactory url is set in the script.
                    withEnv(["MVN_HOME=${mavenHome}"]) {

                        withCredentials([usernamePassword(credentialsId: bitBucketCreds, passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                            sh("ln -s ${projectBitBucketRepo} ${props.build.proxy};chmod 0755 -R *;cd Build;./bundle.sh ${props.build.proxy} ${props.build.version} ${USER} ${PASS} ${apiversion} ${email}")
                        }
                    }

                    // Stash the Project files to be used for deploy and testing
                    echo "STASH FILES"
                    stash includes: FilesToStash(), name: 'AppStash'

                    // Success  send notifications
                    notifyBuild(currentBuild.result, email)
                }

            }
            catch (e) {
                //If there was an exception thrown, the build failed
                currentBuild.result = "FAILED"
                notifyFailed(email)
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
    def email = props.mail
    def buildNumber = env.BUILD_NUMBER
    timestamps {
        // read project.yaml to setup def variables and tools
        // function to get list of Deploy Environments
        def allDeployEnvs = props.deploy
        def bldver = props.build.buildVersion
        def apiversion = "${bldver}.${env.BUILD_NUMBER}"
        echo "allDeployEnvs = ${allDeployEnvs}"
        try {
            // For loop to go through Environments to Deploy
            for (def env : allDeployEnvs) {
                // Stage for each Environments to be Deploy too
                stage("Deploy to ${env.env}") {

                    def ApiEnv = "${env.env}"
                    def approval = env.approval
                    def envType = env.type
                    def ApiOrg = "${env.org}"

                    if (envType == null ) {
                        echo 'Cannot deploy without deploy : env type define example type: Dev, type: QA, type: Prod '
                        currentBuild.rawBuild.result = Result.ABORTED
                        throw new hudson.AbortException('Cannot deploy without ENV type define example type: Dev, type: QA, type: Prod ')
                    }
// approval

                    if (!approval && !(envType.equalsIgnoreCase('PROD')) && !(ApiOrg.equalsIgnoreCase('TMOBILEPRD'))) {
                        echo "ok to Deploy to non-Prod with out any approval"
                    } else if (approval && approval.equalsIgnoreCase('unsecure') && !(envType.equalsIgnoreCase('PROD')) && !(ApiOrg.equalsIgnoreCase('TMOBILEPRD'))) {
                        echo "ok to Deploy to with unsecure non-Prod"
                        Approval(ApiEnv, email)
                    } else if (approval && !(envType.equalsIgnoreCase('PROD')) && !(ApiOrg.equalsIgnoreCase('tmobileprd'))) {
                        echo "ok to Deploy to non-Prod with approval "
                        ApprovalSecured(ApiEnv, approval, email)
                    } else if (approval && approval != null && (envType.equalsIgnoreCase('PROD')) && (ApiOrg.equalsIgnoreCase('TMOBILEPRD'))) {
                        echo "Deploy to Prod "
                        ApprovalSecured(ApiEnv, approval, email)
                    } else {
                        echo 'Cannot deploy to production without approval.'
                        currentBuild.rawBuild.result = Result.ABORTED
                        throw new hudson.AbortException('Cannot deploy to production without approval!')
                        echo 'Further code will not be executed'
                    }
// approval
                    // Launch node
                    node('mesos') {
                        // Clean Jenkins Agent workspace
                        cleanWs notFailBuild: true
                        // unstash project files from Build
                        echo "UNSTASHING FILES"
                        unstash name: 'AppStash'
                        unstash name: 'stashBuildFiles'
                        unstash name: 'stashProxyRaw'
                        unstash name: 'stashDeployable'
                        // setup def variables and tools for eash Environments tobe deployed
                        // def ApiOrg = "${env.org}"
                        def slackChannel = props.slackChannel
                        def ApiHost = "${env.host}"
                        def curlHome = tool 'curl'
                        def xpathHome = tool 'xpath'
                        def zip = tool 'zip'
                        def pomFileLocation = props.pomFileLocation
                        def mavenGoals = props.mavenGoal
                        def nodejs = props.nodeVersion
                        def java = props.java
                        def maven = props.maven
                        def nodeHome = tool name: "${nodejs}", type: 'nodejs'
                        def javaHome = tool name: "${java}", type: 'jdk'
                        def mavenHome = tool name: "${maven}", type: 'maven'
                        def apigeeProxy = props.build.proxy
                        def version = props.build.version
                        def bitBucketCreds = '2d8d909c-6d51-4ffe-8a88-ab8f628e2238'
                        def apigeeCreds = '182f4d7e-7653-4ca2-a54c-a0874471636b'
                        def groovy = tool 'groovy-2.4.9'
                        env.PATH = "${nodeHome}/bin:${groovy}/bin:${env.PATH}"

                        //Notification that we are starting the deployment to test Environments
                        //notifyBuild("Deploying to ${env.env}", email)

                        // Apigee apigee-config for on stash. All Apigee API application will use this cm-apigee-config repo "ACCR" and need to be checkout
                        dir('cm-apigee-config') {
                            git branch: 'master', credentialsId: bitBucketCreds, poll: false, url: 'https://bitbucket.service.edp.t-mobile.com/scm/accr/cm-apigee-config.git'
                        }

                        // call the script deploy-pipe.sh which will download from artifactory and deploy the proxy files to the Apigee Host. Note artifactory URL and Host URL is set in the script.
                        withEnv(["MVN_HOME=${mavenHome}"]) {

                            withCredentials([usernamePassword(credentialsId: apigeeCreds, passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                                def validateTS = sh(script: "groovy Build/VerifyTargetServers.groovy -a 'cm-apigee-config' -b ${buildNumber} -s '${props.build.proxy}/project.yaml' -m", returnStdout: true)
                                println "validateTS = ${validateTS}"
                                def validateResult = readJSON text: validateTS
                                if (validateResult.success) {
                                    if (validateResult.skippedOrgs.get(ApiOrg)) {
                                        println "targetServerUpdate = SKIPPED"
                                    } else {
                                        def targetServerUpdate = sh(script: "cd cm-apigee-config; mvn apigee-config:targetservers -P${ApiOrg}-${ApiEnv} -Dapigee.config.options=update -Dapigee.config.dir=./${ApiOrg} -Dusername=${USER} -Dpassword=${PASS} -Dhttps.protocols=TLSv1.1,TLSv1.2", returnStatus: true)
                                        if (targetServerUpdate == 0) {
                                            println "targetServerUpdate = SUCCESS"
                                        } else {
                                            println "targetServerUpdate = FAILED"
                                            return 1
                                        }
                                    }

                                    def deployProxy = sh(script: "./Build/deploy-pipe.sh ${ApiHost} ${ApiOrg} ${ApiEnv} ${apigeeProxy} ${apiversion} ${USER} ${PASS} ${version}", returnStatus: true)
                                    // println "deployProxy = ${deployProxy}"
                                    if (deployProxy == 0) {
                                        println "deployProxy = SUCCESS"
                                        slackDeployNotify("good", "SUCCESS", ApiOrg, ApiEnv, apigeeProxy, slackChannel)
                                        if (props.testProxy) {
                                            stage("Running Cucumber Report") {
                                                try {
                                                    sh("cd ${apigeeProxy}; mvn -f ${pomFileLocation} ${mavenGoals}")
                                                }
                                                finally {
                                                    cucumber fileIncludePattern: '**/*.json', jsonReportDirectory: "${apigeeProxy}/tests/target/", ignoreFailedTests: 'true'
                                                }
                                            }
                                        }
                                    } else {
                                        //println "deployProxy = FAILED"
                                        slackDeployNotify("danger", "FAILED", ApiOrg, ApiEnv, apigeeProxy, slackChannel)
                                        return 1
                                    }
                                }
                            }
                        }

                        //Success send notifications
                        // notifyBuild(currentBuild.result, email)
                    }

                }
            }

        }
        catch (e) {
            //If there was an exception thrown, the Deploy failed
            currentBuild.result = "FAILED"
            notifyFailed(email)
            throw e
        }

    }

}


// function for project dir and files to stash
def FilesToStash() {
    filesToStash = 'edp-artifacts/**'
    filesToStash += ','
    filesToStash += 'test/**'
    filesToStash += ','
    filesToStash += 'project.yaml'
    filesToStash += ','
    filesToStash += 'Build/**'
    filesToStash += ','
    filesToStash += '${props.build.proxy}/**'
    filesToStash += ','
    filesToStash += 'Deployable/*.zip'
    filesToStash += ','
    filesToStash += '${projectBitBucketRepo}'
    filesToStash += ','
    filesToStash += '${gitBranch}'
}
