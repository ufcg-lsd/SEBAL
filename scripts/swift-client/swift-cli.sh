#!/bin/bash
DIRNAME=`dirname $0`
SWIFT_CONTAINER_NAME=$1
IMAGE_NAME=$2
INPUT_DIRECTORY=$3
OUTPUT_DIRECTORY=$4

## including the openstack environment variables
. $DIRNAME/fogbow-openrc.sh

## generating OS token
mytoken=`openstack token issue -f value -c id`

## storage OS url
storageUrl="https://cloud.lsd.ufcg.edu.br:8080/swift/v1"

function downloadInputs {
	echo "Downloading inputs for image $IMAGE_NAME"
	
	inputPseudFolder="fetcher/inputs"
        grepCommandInputTarget='\'$IMAGE_NAME'.*'tar.gz
        grepCommandInputTarget2='\'$IMAGE_NAME'.*'MTLFmask
        grepCommandInputTarget3='\'$IMAGE_NAME'.*'MTLFmask.hdr
	
	beforeImageFilePath=
        for completeImageFilePath in `swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME | grep -o $grepCommandInputTarget`; do
                if [ "$completeImageFilePath" != "$beforeImageFilePath" ]
		then
			echo "Downloading file $completeImageFilePath"
                	xbase=${completeImageFilePath##*/}
                	swift --os-auth-token $mytoken --os-storage-url $storageUrl download $SWIFT_CONTAINER_NAME $inputPseudFolder/$completeImageFilePath -o $INPUT_DIRECTORY/$xbase
			beforeImageFilePath="$completeImageFilePath"
		fi
        done

	beforeImageFilePath=
        for completeImageFilePath in `swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME | grep -o $grepCommandInputTarget2`; do
                if [ "$completeImageFilePath" != "$beforeImageFilePath" ]
                then
                	echo "Downloading file $completeImageFilePath"
                	xbase=${completeImageFilePath##*/}
                	swift --os-auth-token $mytoken --os-storage-url $storageUrl download $SWIFT_CONTAINER_NAME $inputPseudFolder/$completeImageFilePath -o $INPUT_DIRECTORY/$xbase
			beforeImageFilePath="$completeImageFilePath"
		fi
        done

	beforeImageFilePath=
        for completeImageFilePath in `swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME | grep -o $grepCommandInputTarget3`; do
                if [ "$completeImageFilePath" != "$beforeImageFilePath" ]
                then
                	echo "Downloading file $completeImageFilePath"
                	xbase=${completeImageFilePath##*/}
                	swift --os-auth-token $mytoken --os-storage-url $storageUrl download $SWIFT_CONTAINER_NAME $inputPseudFolder/$completeImageFilePath -o $INPUT_DIRECTORY/$xbase
			beforeImageFilePath="$completeImageFilePath"
		fi
        done
}

function downloadOutputs {
	echo "Downloading outputs for image $IMAGE_NAME"
	
	outputPseudFolder="fetcher/images"
        grepCommandTarget='\'$IMAGE_NAME'.*'nc

	beforeImageFilePath=
        for completeImageFilePath in `swift --os-auth-token $mytoken --os-storage-url $storageUrl list $SWIFT_CONTAINER_NAME | grep -o $grepCommandTarget`; do
                if [ "$completeImageFilePath" != "$beforeImageFilePath" ]
                then
                	echo "Downloading file $completeImageFilePath"
                	xbase=${completeImageFilePath##*/}
                	swift --os-auth-token $mytoken --os-storage-url $storageUrl download $SWIFT_CONTAINER_NAME $outputPseudFolder/$completeImageFilePath -o $OUTPUT_DIRECTORY/$xbase
			beforeImageFilePath="$completeImageFilePath"
		fi
        done
}

function main {
	if [[ -z $IMAGE_NAME || -z $SWIFT_CONTAINER_NAME ]]; then
        	echo "Please, inform the CONTAINER_NAME and the IMAGE_NAME to download"
        	echo "USAGE: bash swift-cli.sh CONTAINER_NAME IMAGE_NAME"
        	exit 1
	else
        	downloadInputs
	        downloadOutputs
	fi
}

main
