#!/usr/bin/env bash

# print-environment.sh
#
# Prints the environment for CI build logs, like `printenv | sort`, but masks
# the values of variables that look like they hold credentials: those with a
# credential-like name, and the user name and password in URLs.

# a trace would print each value before it is masked
set +x

shopt -s nocasematch

SENSITIVE_NAME='KEY|TOKEN|SECRET|PASS|PSW|CRED|AUTH|LOGIN|COOKIE|PRIVATE|CERT|(^|_)PAT(_|$)|_PWD(_|$)'

# read a snapshot of the environment, so that the loop's own variables can't
# shadow exported variables of the same name
env -0 | sort -z | while IFS= read -r -d '' entry; do

   name="${entry%%=*}"
   value="${entry#*=}"

   # user names and passwords in URLs run through the last '@' before the
   # path, since they can hold an unescaped '@'; proxy settings can leave out
   # the scheme, as in user:pass@proxy:3128
   if [[ "${name}" =~ ${SENSITIVE_NAME} ]]; then
      value="<masked>"
   elif [[ "${value}" == *@* ]]; then
      value="$(printf '%s' "${value}" | sed -E \
         -e 's,://[^/?#[:space:]]*@,://<masked>@,g' \
         -e 's,(^|[[:space:]=])[^[:space:]/?#:@=]+:[^[:space:]/?#]*@,\1<masked>@,g')"
   fi

   printf '%s=%s\n' "${name}" "${value}"

done
