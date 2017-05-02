#!/bin/bash

# ${IMAGE_NAME}
IMAGE_NAME=$1
# ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/
IMAGES_DIR_PATH=$2
# ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/
RESULTS_DIR_PATH=$3
# ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}
OUTPUT_IMAGE_DIR=$4
# ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTL.txt"
IMAGE_MTL_PATH=$5
# ${SEBAL_MOUNT_POINT}/$IMAGES_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_MTLFmask"
IMAGE_MTL_FMASK_PATH=$6
# ${SEBAL_MOUNT_POINT}/$RESULTS_DIR_NAME/${IMAGE_NAME}/${IMAGE_NAME}"_station.csv"
IMAGE_STATION_FILE_PATH=$7

function run {
    #we should parametrize this
    sleep 600
}

function generate_fake_data {
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_alb.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_EF.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_ET24h.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_EVI.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_G.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_LAI.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_NDVI.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_Rn.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_TS.nc bs=1m count=159
  dd if=/dev/urandom of=$IMAGE_STATION_FILE_PATH/$IMAGE_NAME_station.csv bs=1 count=143
}

# This function ends the script
function finally {
  exit 0
}

run
generate_fake_data
finally
