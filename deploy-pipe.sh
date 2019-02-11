#!/bin/bash
set +v
set -e

autopath=`pwd`

workspace=$1
projectName=$2
type=$3
sharedflowPomPath=$1/src/sharedflows/$2
proxyPomPath=$1/src/gateway/$2

host=$1
org=$2
env=$3
application=$4
version=$5
username=$6
password=$7
buildNumber=$8

artifactoryURLForProxy='http://demo.itorix.com:8081/artifactory/apigee-proxy-build'

buildSharedFlow(){
	cd $autopath
	echo "Maven Clean:"
	mvn clean -X -f $sharedflowPomPath/pom.xml

	cd $autopath
	echo "Bundling SharedFlow:" $projectName
	mvn package -X -f $sharedflowPomPath/pom.xml -Ptest
}

buildProxy(){
	cd $autopath
	echo "Maven Clean:"
	mvn clean -X -f $sharedflowPomPath/pom.xml

	cd $autopath
	echo "Bundling Proxy:" $projectName
	mvn package -X -f $proxyPomPath/pom.xml -Ptest
}

main(){
	echo "Type is missing in the project.yaml"
}

response(){
	echo "Build stage is complete"
}

if [[ $type == "sharedflow" ]]
	then
		buildSharedFlow
elif [[ $type == "proxy" ]]
		then
			buildProxy
else
	main
fi

if [[ $projectName != "" ]]
	then
		main
else
	response
fi
