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
sharedflowTargetPath=$workspaceDirectory/src/sharedflows/$projectName
proxyProxyPath=$workspaceDirectory/src/gateway/$projectName

uploadSharedFlow(){
echo "************Deploying ProxyName to Artifactory:" $projectName"-"$buildVersion
export "credentials=$username:$password"
cd $sharedflowTargetPath/target
curl -sk -u $credentials -T $proxyName"-"$buildVersion".zip" -X PUT "$artifactoryURLForSharedFlow/$apiversion/$proxyName-$apiversion.zip"
echo "************Deployed successfully to artifactory:" $proxyName-$buildVersion"-"$apiversion
}

uploadProxy(){
echo "************Deploying ProxyName to Artifactory:" $projectName"-"$buildVersion
export "credentials=$username:$password"
cd $proxyProxyPath/target
curl -sk -u $credentials -T $proxyName"-"$buildVersion".zip" -X PUT "$artifactoryURLForProxy/$apiversion/$proxyName-$apiversion.zip"
echo "************Deployed successfully to artifactory:" $proxyName-$buildVersion"-"$apiversion
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
