#!/usr/bin/env bash

# print-environment.sh
#
# Prints the environment for CI build logs, like `printenv | sort`, but masks
# the values of variables that look like they hold credentials: those with a
# credential-like name, and the user name and password in URLs.

# a trace would print each value before it is masked
set +x

set -o pipefail
shopt -s nocasematch

# credential-like names. GIT_CONFIG_PARAMETERS and GIT_CONFIG_VALUE_<n> can
# carry settings such as http.extraHeader, and exported functions
# (BASH_FUNC_*) can hold anything
SENSITIVE_NAMES=(
   'AUTH'
   '^BASH_FUNC_'
   'CERT'
   'CONNECTION_STRING'
   'COOKIE'
   'CRED'
   '(^|_)DSN(_|$)'
   '^GIT_CONFIG_PARAMETERS'
   '^GIT_CONFIG_VALUE_'
   'KEY'
   'LOGIN'
   'PASS'
   '(^|_)PAT(_|$)'
   'PRIVATE'
   'PSW'
   '_PWD(_|$)'
   'SECRET'
   'TOKEN'
   'WEBHOOK'
)
SENSITIVE_NAME=$(IFS='|'; echo "${SENSITIVE_NAMES[*]}")

# a password parameter, as in a connection string or a URL query
PASSWORD_PARAMETER='(pass(wd|word)?|pwd)='

# proxy endpoints, such as HTTPS_PROXY or COPILOT_PROXY_URL
PROXY_NAME='PROXY(_URL)?$'

# read a snapshot of the environment, so that the loop's own variables can't
# shadow exported variables of the same name
env -0 | sort -z | while IFS= read -r -d '' entry; do

   name="${entry%%=*}"
   value="${entry#*=}"

   if [[ "${name}" =~ ${SENSITIVE_NAME} || "${value}" =~ ${PASSWORD_PARAMETER} ]]; then
      value="<masked>"
   elif [[ "${name}" =~ ${PROXY_NAME} && "${value}" == *@* ]]; then
      # proxy settings can leave out the scheme (user:pass@proxy:3128), and
      # their user names can hold an '@' (user@domain:pass@proxy:3128)
      scheme=""
      if [[ "${value}" == *://* ]]; then
         scheme="${value%%://*}://"
      fi
      value="${scheme}<masked>@${value##*@}"
   elif [[ "${value}" == *://*@* ]]; then
      # user names and passwords can hold an unescaped '@', so they run
      # through the last '@' before the path
      value="$(printf '%s' "${value}" | sed -E 's,://[^/?#[:space:]]*@,://<masked>@,g')"
   fi

   printf '%s=%s\n' "${name}" "${value}"

done
