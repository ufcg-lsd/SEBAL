#!/bin/bash
DIRNAME=`dirname $0`
SWIFT_CONTAINER_NAME=$1
OPTION=$2

## including the openstack environment variables
. $DIRNAME/fogbow-openrc.sh

## generating OS token
mytoken=`openstack token issue -f value -c id`

## storage OS url
storageUrl="https://cloud.lsd.ufcg.edu.br:8080/swift/v1"

function listFilesInContainer {
	if [ $SWIFT_CONTAINER_NAME != "" ]
	then
		if [ "$OPTION" == "" ]
		then
			echo "Listing all files in $SWIFT_CONTAINER_NAME"
                        swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME
		fi

		if [ "$OPTION" == "inputs" ]
		then
			echo "Listing input files in $SWIFT_CONTAINER_NAME"
			swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME --prefix "fetcher/inputs"
		fi

		if [ "$OPTION" == "outputs" ]
		then
			echo "Listing output files in $SWIFT_CONTAINER_NAME"
			swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME --prefix "fetcher/images"
		fi
	else
		echo "Invalid swift container name $SWIFT_CONTAINER_NAME"
		exit 1
	fi
	
}

listFilesInContainer
