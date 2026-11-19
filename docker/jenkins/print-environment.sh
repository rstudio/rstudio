#!/usr/bin/env bash

# print-environment.sh
#
# Prints the environment for CI build logs, like `printenv | sort`, but masks
# the values of variables that look like they hold credentials: those with a
# credential-like name, and the user name and password in URLs.

shopt -s nocasematch

SENSITIVE_NAME='KEY|TOKEN|SECRET|PASS|PSW|CRED|AUTH|LOGIN|COOKIE|PRIVATE|CERT|(^|_)PAT(_|$)'
URL_USERINFO='://[^/@[:space:]]+@'

for NAME in $(compgen -e | sort); do

   VALUE="${!NAME}"

   if [[ "${NAME}" =~ ${SENSITIVE_NAME} ]]; then
      VALUE="<masked>"
   elif [[ "${VALUE}" =~ ${URL_USERINFO} ]]; then
      VALUE="$(printf '%s' "${VALUE}" | sed -E 's#://[^/@[:space:]]+@#://<masked>@#g')"
   fi

   printf '%s=%s\n' "${NAME}" "${VALUE}"

done
