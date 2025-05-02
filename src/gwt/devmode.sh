#!/usr/bin/env bash

PID=$(pgrep -f RStudioSuperDevMode)
if [ -n "${PID}" ]; then
	exit
fi

rm -f gwt-code-server.log
mkfifo gwt-code-server.log

nohup ant devmode &> gwt-code-server.log &
PID=$(pgrep -f RStudioSuperDevMode)

READY="Server:main: Started"
while IFS= read -r LINE; do
	echo "${LINE}"
	case "${LINE}" in
	*"${READY}"*) break ;;
	esac
done < gwt-code-server.log
