#!/bin/bash
IMAGE_NAME=$1

TEMP_URL_EXPIRATION_TIME=10800


SWIFT_URL= # Until 8080
SWIFT_TEMP_URL_KEY=

echo "Getting $IMAGE_NAME temporary swift input file url"
image_temp_url_endpoint=$(swift tempurl GET $TEMP_URL_EXPIRATION_TIME /swift/v1/sebal_container/fetcher/inputs/$IMAGE_NAME/$IMAGE_NAME".tar.gz" $SWIFT_TEMP_URL_KEY)
image_full_temp_url=$SWIFT_URL$image_temp_url_endpoint
echo "Temporary input download URL is $image_full_temp_url"

echo "Downloading input files for image $IMAGE_NAME"
curl -L -o ./$IMAGE_NAME".tar.gz" -X GET $image_full_temp_url
echo -e "\nDownload completed!"
