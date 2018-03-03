#!/bin/bash

export LC_ALL="C"
TIME_BETWEEN_COMMANDS=1
echo TIMESTAMP, RUNNING_THREADS
while [ -e /proc/$1 ]; do
	/usr/bin/dstat -p $TIME_BETWEEN_COMMANDS 1 | awk -v date="$( date +"%s" )" 'NR==4 { print date", "$1}'
	#sleep $TIME_BETWEEN_COMMANDS
done

