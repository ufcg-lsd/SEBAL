#!/bin/bash

SEBAL_SCRIPTS_PATH=$1
MISSING_DEPENDENCIES_FILE=missing_dependencies

Rscript $SEBAL_SCRIPTS_PATH/check-R-dependency.R raster
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing raster dependency" >> $MISSING_DEPENDENCIES_FILE
else
  echo "raster is installed"
fi

Rscript $SEBAL_SCRIPTS_PATH/check-R-dependency.R rgeos
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing rgeos dependency" >> $MISSING_DEPENDENCIES_FILE
else
  echo "rgeos is installed"
fi

Rscript $SEBAL_SCRIPTS_PATH/check-R-dependency.R rgdal
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing rgdal dependency" >> $MISSING_DEPENDENCIES_FILE
else
  echo "rgdal is installed"
fi

Rscript $SEBAL_SCRIPTS_PATH/check-R-dependency.R maptools
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing maptools dependency" >> $MISSING_DEPENDENCIES_FILE
else
  echo "maptools is installed"
fi

Rscript $SEBAL_SCRIPTS_PATH/check-R-dependency.R ncdf4
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing ncdf4 dependency" >> $MISSING_DEPENDENCIES_FILE
else
  echo "ncdf4 is installed"
fi

Rscript $SEBAL_SCRIPTS_PATH/check-R-dependency.R sp
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing sp dependency" >> $MISSING_DEPENDENCIES_FILE
else
  echo "sp is installed"
fi
