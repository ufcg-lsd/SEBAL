#!/bin/bash

# Global variables
LOG4J_FILE_PATH=/var/log/sebal/sebal.log
SEBAL_DIR_PATH=$(pwd)
TEMP_DIR_PATH=/mnt
PIP_VERSION=https://bootstrap.pypa.io/get-pip.py
SWIFT_CLIENT_VERSION=3.1.0

# TODO: fix this
SEBAL_SNAPSHOT_M2_PATH=/home/fogbow/.m2/repository/org/fogbowcloud/SEBAL/0.0.1-SNAPSHOT/

function gettingSebalSnapshot {
  cd $SEBAL_DIR_PATH/SEBAL
  # getting sebal snapshot from public_html
  sudo tar -xvzf target.tar.gz
  rm target.tar.gz

  # putting snapshot into .m2
  sudo mkdir -p $SEBAL_SNAPSHOT_M2_PATH
  sudo cp $SEBAL_DIR_PATH/SEBAL/target/SEBAL-0.0.1-SNAPSHOT.jar $SEBAL_SNAPSHOT_M2_PATH

  sudo mkdir -p $LOG4J_FILE_PATH
  cd ..
}

function createREnvFile {
  sudo touch $TEMP_DIR_PATH/.Renviron
}

function installSwiftDependencies {
  sudo apt update
  sudo apt install python-virtualenv
  sudo ln -s /usr/lib/python2.7/plat-*/_sysconfigdata_nd.py /usr/lib/python2.7/
  mkdir swift-client
  virtualenv swift-client/
  . swift-client/bin/activate
  wget $PIP_VERSION
  python get-pip.py
  pip install python-swiftclient==$SWIFT_CLIENT_VERSION
}

function verifyRScript {
  echo "Verifying dependencies for R script"
  
  # TODO: put this in our SEBAL repository
  bash -x ${SEBAL_DIR_PATH}/SEBAL/scripts/verify-dependencies.sh ${SEBAL_DIR_PATH}/SEBAL/scripts
}

gettingSebalSnapshot
installSwiftDependencies
verifyRScript
