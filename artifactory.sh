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
buildNumber=$10

echo "$workspaceDirectory"
echo "$projectName"
echo "$buildType"
echo "$apiversion"
echo "$artifactoryURLForSharedFlow"
echo "$artifactoryURLForProxy"
echo "$username"
echo "$version"
echo "$buildNumber"

sharedflowTargetPath=$workspaceDirectory/src/sharedflows/$projectName
proxyProxyPath=$workspaceDirectory/src/gateway/$projectName

uploadSharedFlow(){
echo "************Deploying sharedflow to Artifactory:" $projectName"-"$version
curl -u$username:$password -T $sharedflowTargetPath/target/$projectName"-"$version".zip" "$artifactoryURLForSharedFlow/$proxyName-$version/$buildNumber"
echo "************Deployed successfully to artifactory:" $proxyName-$version"-"$apiversion
}

uploadProxy(){
	echo "Proxy Upload"
}

main(){
	echo "Type or project name is missing in the project.yaml"
}

if [[ $buildType == "sharedflow" ]]
	then
		uploadSharedFlow
elif [[ $buildType == "proxy" ]]
		then
			uploadProxy
else
	main
fi

if [[ $projectName != "" ]]
	then
		main
else
	main
fi
