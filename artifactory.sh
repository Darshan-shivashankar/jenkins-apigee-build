#!/bin/bash
set +v
set -e

autopath=`pwd`

workspaceDirectory=$1
projectName=$2
buildType=$3
apiversion=$4
artifactoryURLForSharedFlow=$5
artifactoryURLForProxy=$6
username=$7
password=$8
buildVersion=$9
buildNumber=$10

sharedflowTargetPath=$workspaceDirectory/src/sharedflows/$projectName
proxyProxyPath=$workspaceDirectory/src/gateway/$projectName

uploadSharedFlow(){
echo "************Deploying sharedflow to Artifactory:" $projectName"-"$buildVersion
curl -u$username:$password -T $sharedflowTargetPath/target/$projectName"-"$buildVersion".zip" "$artifactoryURLForSharedFlow/$proxyName-$buildVersion/$buildNumber"
echo "************Deployed successfully to artifactory:" $proxyName-$buildVersion"-"$apiversion
}

uploadProxy(){

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
