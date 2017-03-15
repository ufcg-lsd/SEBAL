#!/bin/bash

cmd=$(top -bn1 | grep "load") 2> /dev/null
while true; do
    echo "$(date +%s)#${cmd}"
    sleep 1
done
