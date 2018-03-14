#!/bin/bash

export LC_ALL="C"
TIME_BETWEEN_COMMANDS=1
echo TIMESTAMP, AVG_QUEUE_SIZE
while [ -e /proc/$1 ]; do
        /usr/bin/iostat -xz $TIME_BETWEEN_COMMANDS 1 | grep "avgqu-sz" -A 1 | awk -v date="$( date +"%s" )" 'NR==2 { print date", "$9}'
        sleep $TIME_BETWEEN_COMMANDS
done
