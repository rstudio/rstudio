#!/bin/sh
name=$1
count=$(grep -r  @${name} test |wc -l)
echo "$((${count} -1))"


