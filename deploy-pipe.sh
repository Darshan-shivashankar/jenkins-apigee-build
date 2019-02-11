#!/bin/bash
set +v
set -e

autopath=`pwd`

host=$1
org=$2
env=$3
application=$4
version=$5
artifactoryUser=$6
artifactoryPassword=$7
buildNumber=$8
workspace=$9
buildType=${10}
artifactoryURLForSharedFlow=${11}
artifactoryURLForProxy=${12}
apigeeUser=${13}
apigeePassword=${14}


sharedflowPomPath=$9/src/sharedflows/$4
proxyPomPath=$9/src/gateway/$4

function downloadSharedFlowFile(){

cd $sharedflowPomPath/target
rm -rf "$application-$version.zip"
echo "downloading the latest $application-$version bundle from Artifactory"
echo "download link - $artifactoryURLForSharedFlow/$application-$version/$buildNumber/$application-$version.zip"
curl -u $artifactoryUser:$artifactoryPassword -O "$artifactoryURLForSharedFlow/$application-$version/$buildNumber/$application-$version.zip"
chmod 755 "$application-$version.zip"

}

function downloadProxyFile(){

	cd $proxyPomPath/target
	rm -rf "$application-$version.zip"

	echo "downloading the latest $application-$version bundle from Artifactory"
	echo "download link - $artifactoryURLForProxy/$application-$version/$buildNumber/$application-$version.zip"
	curl -u $artifactoryUser:$artifactoryPassword -O "$artifactoryURLForProxy/$application-$version/$buildNumber/$application-$version.zip"

	chmod 755 "$application-$version.zip"

}

deploySharedFlow(){

	cd $autopath
	downloadSharedFlowFile
	echo "Deploying SharedFlow:" $application
	mvn apigee-enterprise:deploy -X -f $sharedflowPomPath/pom.xml -P$env -Dusername=$apigeeUser -Dpassword=$apigeePassword
}

deployProxy(){

	cd $autopath
	downloadProxyFile
	echo "Deploying Proxy:" $application
	mvn apigee-enterprise:deploy -X -f $proxyPomPath/pom.xml -P$env -Dusername=$apigeeUser -Dpassword=$apigeePassword
}

main(){
	echo "Type is missing in the project.yaml"
}

response(){
	echo "Build stage is complete"
}

if [[ $buildType == "sharedflow" ]]
	then
		deploySharedFlow
elif [[ $buildType == "proxy" ]]
		then
			deployProxy
else
	main
fi

if [[ $projectName != "" ]]
	then
		response
else
	response
fi
