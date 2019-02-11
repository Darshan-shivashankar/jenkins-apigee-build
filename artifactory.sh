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
curl -sk -u $username:$password -T $sharedflowTargetPath/target/$projectName"-"$version".zip" -X PUT "http://demo.itorix.com:8081/artifactory/apigee-sharedflow-build/$proxyName-$version/$buildNumber/$proxyName-$version.zip"
echo "************Deployed successfully to artifactory:" $proxyName-$version"-"$apiversion
}

uploadProxyFlow(){
echo "************Deploying Proxy to Artifactory:" $projectName"-"$version
curl -sk -u $username:$password -T $proxyProxyPath/target/$projectName"-"$version".zip" -X PUT "http://demo.itorix.com:8081/artifactory/apigee-proxy-build/$proxyName-$version/$buildNumber/$proxyName-$version.zip"
echo "************Deployed successfully to artifactory:" $proxyName-$version"-"$apiversion
}

main(){
	echo "Type is missing in the project.yaml"
}

response(){
	echo "Upload stage is complete"
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
