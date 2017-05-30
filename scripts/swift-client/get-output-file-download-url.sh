#!/bin/bash
IMAGE_NAME=$1
VARIABLE_NAME=$2

TEMP_URL_EXPIRATION_TIME=10800

SWIFT_URL= # Until 8080
SWIFT_TEMP_URL_KEY=

echo "Getting $IMAGE_NAME temporary swift $VARIABLE_NAME file url"
image_temp_url_endpoint=$(swift tempurl GET $TEMP_URL_EXPIRATION_TIME /swift/v1/sebal_container/fetcher/images/$IMAGE_NAME/$IMAGE_NAME"_"$VARIABLE_NAME".nc" $SWIFT_TEMP_URL_KEY)
image_full_temp_url=$SWIFT_URL$image_temp_url_endpoint
echo "Temporary $VARIABLE_NAME file for image $IMAGE_NAME download URL is $image_full_temp_url"

echo "Downloading $VARIABLE_NAME file for $IMAGE_NAME"
curl -L -o ./$IMAGE_NAME"_"$VARIABLE_NAME".nc" -X GET $image_full_temp_url
echo -e "\nDownload completed!"
