#!/bin/bash

export LC_ALL="C"
TIME_BETWEEN_COMMANDS=1
echo TIMESTAMP, READ, WRITE
while [ -e /proc/$1 ]; do
    cmd=$(nfsiostat $TIME_BETWEEN_COMMANDS 2)
    echo ${cmd} | awk -v date="$( date +"%s" )" '{ print date", "$67", "$85 }' 2> /dev/null
done
