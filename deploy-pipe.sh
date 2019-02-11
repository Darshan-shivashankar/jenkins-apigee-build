#!/bin/bash
host=$1
org=$2
env=$3
application=$4
apiversion=$5
username=$6
password=$7
credentials=$username:$password
version=$8

function importanddeploy()  {

		#rm -rf $application.zip

		#Create the bundle and deploy
		#zip -r $application.zip apiproxy

		echo "downloading the latest $application-$apiversion bundle from Artifactory"
		echo "download link - https://artifactory.service.edp.t-mobile.com/artifactory/tmo-releases/com/tmobile/apigee-coreapi/$application/$version/$apiversion/$application-$apiversion.zip"
              download=`wget "https://artifactory.service.edp.t-mobile.com/artifactory/tmo-releases/com/tmobile/apigee-coreapi/$application/$version/$apiversion/$application-$apiversion.zip" 2>/dev/null`

		chmod 755 $application-$apiversion.zip

		echo "Getting the current deployed version"
		apis=`curl -k -X GET -H "Accept: application/xml" -u $credentials "$host/v1/organizations/$org/environments/$env/deployments" 2>/dev/null`
		echo $apis > temp.xml

        deployedVersion=$(xpath -e "//APIProxy[@name='$application']/Revision/@name" temp.xml 2> /dev/null)
       	deployedVersion=${deployedVersion//name=/}
        deployedVersion=${deployedVersion//\"/}
        deployedVersion=${deployedVersion//\ /}

    echo "dep version="$deployedVersion

		echo "Importing new revision"

		#!echo curl -k -s $credentials "$host/v1/organizations/$org/apis?action=import&name=$application" -T $application-$version-$apiversion.zip -H "Content-Type: application/octet-stream" -X POST
		#!imprt=`curl -k -s $credentials "$host/v1/organizations/$org/apis?action=import&name=$application" -T $application-$version-$apiversion.zip -H "Accept: application/xml" -H "Content-Type: multipart/form-data" -X POST 2>/dev/null`

		echo curl -k -s -u $credentials "$host/v1/organizations/$org/apis?action=import&name=$application" -F "file=@$application-$apiversion.zip" -H "Accept: application/xml" -H "Content-Type: multipart/form-data" -X POST
		imprt=`curl -k -s -u $credentials "$host/v1/organizations/$org/apis?action=import&name=$application" -F "file=@$application-$apiversion.zip" -H "Accept: application/xml" -H "Content-Type: multipart/form-data" -X POST 2>/dev/null`
		echo $imprt > dep.xml


          revision=$(xpath -e '//APIProxy/@revision' dep.xml 2> /dev/null)
        	revision=${revision/revision=/}
        	revision=${revision//\"/}
        	revision=${revision//\ /}
       	    echo "New Revision imported= $revision"



		    echo "undeploy this $deployedVersion  revision"

        	undeploy=`curl -k -s -X POST -u $credentials "$host/v1/organizations/$org/apis/$application/deployments?action=undeploy&env=$env&revision=$deployedVersion" 2>/dev/null`
        	echo $undeploy

        	echo "Waiting for undeploying"

        	sleep 2

		    echo "deploy this $revision revision"

        	deploy=`curl -k -s -X POST -u $credentials "$host/v1/organizations/$org/apis/$application/revisions/$revision/deployments?action=deploy&env=$env&override=true" 2>/dev/null`
        	echo $deploy

        rm -fr dep.xml
		rm -fr temp.xml
		rm -fr $application-$apiversion.zip

}

#IntTesting(){
#    echo "Integration Test Execution"
#    mvn -e exec:exec@integration -f test-pom.xml -DtestDir=$basepathdir/$proxyName/tests/$version
#    STATUS=$?
#    if [ $STATUS -eq 0 ]; then
#        echo "Integration Testing Successful"
#    else
#        exit
#    fi
#}

importanddeploy
		content=`curl -k -siI -X GET "$host/v1/o/$org/e/$env/apis/$application/revisions/$revision/deployments" -H 'Content-type:application/xml' -u $credentials`
                        httpStatus=$(echo "${content}" | grep '^HTTP/1' | awk {'print $2'} |tail -1)
                        echo $httpStatus
                        if [[ httpStatus -eq 200 ]]
                        then
                        echo --------------------------------------------------------
                        echo $application deployed successfully
                        echo --------------------------------------------------------
#                        IntTesting
                        exit 0
                        else
                        echo -----------------------------------------
                        echo $application NOT deployed successfully
                        echo ----------------------------------------
                        exit 1
                        fi

#done
exit
