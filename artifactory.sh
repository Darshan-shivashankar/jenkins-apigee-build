#!/bin/bash
set +v
set -e

autopath=`pwd`

version=$1
workspaceDirectory=$2
projectName=$3
buildType=$4
artifactoryNumber=$5
artifactoryURLForSharedFlow=$6
artifactoryURLForProxy=$7
username=$8
password=$9

sharedflowTargetPath=$workspaceDirectory/src/sharedflows/$projectName
proxyProxyPath=$workspaceDirectory/src/gateway/$projectName

uploadSharedFlow(){
echo "************ Jenkins Build Number :"$artifactoryNumber
echo "************Deploying sharedflow to Artifactory:" $projectName"-"$version
curl -X PUT -u $username:$password -T $sharedflowTargetPath/target/$projectName"-"$version".zip" "$artifactoryURLForSharedFlow/$projectName-$version/$artifactoryNumber/$projectName-$version.zip"
echo "************Deployed successfully to artifactory:" $projectName-$version"-"$apiversion
}

uploadProxyFlow(){
echo "************ Jenkins Build Number :"$artifactoryNumber
echo "************Deploying Proxy to Artifactory:" $projectName"-"$version
curl -X PUT -u $username:$password -T $sharedflowTargetPath/target/$projectName"-"$version".zip" "$artifactoryURLForProxy/$projectName-$version/$artifactoryNumber/$projectName-$version.zip"
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
