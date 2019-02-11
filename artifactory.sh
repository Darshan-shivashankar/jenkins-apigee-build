#!/bin/bash
set +v
set -e

autopath=`pwd`

version=$1
workspaceDirectory=$2
projectName=$3
buildType=$4
apiversion=$5
artifactoryURLForSharedFlow=$6
artifactoryURLForProxy=$7
username=$8
password=$9
artifactoryNumber=$10

sharedflowTargetPath=$workspaceDirectory/src/sharedflows/$projectName
proxyProxyPath=$workspaceDirectory/src/gateway/$projectName

uploadSharedFlow(){
echo "************Deploying sharedflow to Artifactory:" $projectName"-"$version
curl -X PUT -u admin:password -T $sharedflowTargetPath/target/$projectName"-"$version".zip" "http://demo.itorix.com:8081/artifactory/apigee-sharedflow-build/$projectName-$version/$buildNumber/$projectName-$version.zip"
echo "************Deployed successfully to artifactory:" $projectName-$version"-"$apiversion
}

uploadProxyFlow(){
echo "************Deploying Proxy to Artifactory:" $projectName"-"$version
curl -sk -u admin:password -T $proxyProxyPath/target/$projectName"-"$version".zip" -X PUT "http://demo.itorix.com:8081/artifactory/apigee-proxy-build/$proxyName-$version/$buildNumber/$projectName-$version.zip"
echo "************Deployed successfully to artifactory:" $projectName-$version"-"$apiversion
}

main(){
	echo "Type is missing in the project.yaml"
}

response(){
	echo "Upload stage is complete"
}

if [[ $buildType == "sharedflow" ]]
	then
		uploadSharedFlow
elif [[ $buildType == "proxy" ]]
		then
			uploadProxyFlow
else
	main
fi

if [[ $projectName != "" ]]
	then
		main
else
	response
fi
