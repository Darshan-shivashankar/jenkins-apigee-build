#!/bin/bash
set +v
set -e

autopath=`pwd`

workspace=$1
proxyName=$2
type=$3
sharedflowPomPath=$1/src/sharedflows/$2
proxyPomPath=$1/src/gateway/$2

buildSharedFlow(){
	cd $autopath
	echo "Maven Clean:"
	mvn clean -X -f $sharedflowPomPath/pom.xml

	cd $autopath
	echo "Bundling SharedFlow:" $proxyName
	mvn package -X -f $sharedflowPomPath/pom.xml -Ptest
}

buildProxy(){
	cd $autopath
	echo "Maven Clean:"
	mvn clean -X -f $sharedflowPomPath/pom.xml

	cd $autopath
	echo "Bundling Proxy:" $proxyName
	mvn package -X -f $proxyPomPath/pom.xml -Ptest
}

main(){
	echo "Type is missing in the project.yaml"
}

if [[ $type == "sharedflow" ]]
	then
		buildSharedFlow
elif [[ $type == "proxy" ]]
		then
			buildProxy
else
	main
