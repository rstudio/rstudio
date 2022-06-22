#!/usr/bin/env bash
# REPORTS THE USAGE OF SERVER AND SESSION OPTIONS, pairs with generate-options.R
###############################################################################
# We look in the documentation for 3 ways in which an option may be used:
# - A mention like `auth-none`
# - A command like `--auth-none`
# - A example of config file like:
# ```
# auth-none=1
# ```
# If none of these are present in the Rmd files, we warn for non-hidden, non-deprecated options.
#
# To see all options instead of just the ones being misused, run this script with the `ALL=1` variable.
###############################################################################
ALL=$1

function inDocs() {
	line=$1
	MENTION_COUNT=$(grep -l -w "\`$line\`" ../../docs/server/*/*.qmd | wc -l)
	EXAMPLE_COUNT=$(grep -l -e "^\b$line\b" ../../docs/server/*/*.qmd | wc -l)
	COMMAND_COUNT=$(grep -l -w "\-\-$line" ../../docs/server/*/*.qmd | wc -l)
	if [[ $MENTION_COUNT -ne 0 || $EXAMPLE_COUNT -ne 0 || $COMMAND_COUNT -ne 0 ]]; then
		return 1
	fi
	return 0
}

function inCode() {
	COUNT=$(find ./ -name *.cpp -o -name *.hpp | grep -v '.gen' | xargs grep -w $line | wc -l)
	if [[ $COUNT -eq 0 ]]; then
		return 0
	fi
	return 1
}

function listDocFiles() {
	grep -l -w "$line" ../../docs/server/*.Rmd | cut -d':' -f 1 | tr '\n' ' '
}

function scanPublicOptions() {
	FILE="$1"
	OPTIONS=$(cat ./$FILE/$FILE-options*.json | jq -r '.options | keys[] as $k | .[$k] | flatten |  .[] | select(.isHidden != true and .isDeprecated != true) | .name | if type=="object" then .value else . end' | sort)
	while IFS= read -r line; do
		inDocs $line
		if [[ $? -eq 0 ]]; then
			echo "$FILE: $line -- MISSING IN DOCS, ADD IT TO DOCS, MAKE IT HIDDEN OR DEPRECATED"
		elif [[ ! -z $ALL ]]; then
			FILES=$(listDocFiles)
			echo "$FILE: $line -- IN DOCS $FILES, OK"
		fi
	done <<< $OPTIONS
}

function serverOptionsMatch() {
	OPTION="$1"
	SERVER_OPTIONS=$(cat ./server/server-options*.json | jq -r '.options | keys[] as $k | .[$k] | flatten |  .[] | .name | if type=="object" then .value else . end' | sort)
	echo "$SERVER_OPTIONS" | grep -l -w $OPTION | wc -l
}

function scanSomeOptions() {
	FILE="$1"
	TYPE="$2"
	OPTIONS=$(cat ./$FILE/$FILE-options*.json | jq --arg field "$TYPE" -r '.options | keys[] as $k | .[$k] | flatten |  .[] | select(.[$field] == true) | .name | if type=="object" then .value else . end' | sort)
	while IFS= read -r line; do
		inDocs $line
		if [[ $? -eq 1 ]]; then
			FILES=$(listDocFiles)
			if [[ $FILE == "session" ]]; then
				OTHER=$(serverOptionsMatch $line)
			else
				OTHER=0
			fi
			if [[ $TYPE == "isHidden" ]]; then
				VISIBILITY="VISIBLE"
			else
				VISIBILITY="HIDDEN"
			fi
			if [[ $OTHER -eq 0 ]]; then
				echo "$FILE: $line -- $TYPE BUT SHOWN IN $FILES, MAKE IT $VISIBILITY OR REMOVE IT FROM DOCS"
			elif [[ ! -z $ALL ]]; then
				echo "$FILE: $line -- $TYPE IN SESSION BUT ALSO IN SERVER, SHOWN IN $FILES, PLEASE CHECK"
			fi
		elif [[ ! -z $ALL ]]; then
			echo "$FILE: $line -- $TYPE AND NOT IN DOCS, OK"
		fi
	done <<< $OPTIONS
}

function scanCodeOptions() {
	FILE=$1
	OPTIONS=$(cat ./$FILE/$FILE-options*.json | jq -r '.options | keys[] as $k | .[$k] | flatten |  .[] | if .accessorName then .accessorName else (if .memberName then .memberName|sub("_"; "") else (if .name|type=="object" then .name.constant else . end) end) end' | sort)
	while IFS= read -r line; do
		COUNT=$(find ./ -name *.cpp -o -name *.hpp | grep -v '.gen' | xargs grep -w $line | wc -l)
		if [[ $COUNT -eq 0 ]]; then
			echo "$FILE: $line -- NOT IN CODE, MAKE IT DEPRECATED AND HIDDEN"
		fi
	done <<< $OPTIONS
}

echo "## Public Options"

# Indicates whether a public server option (not deprecated or hidden) is present in the documentation
scanPublicOptions "server"

# Indicates whether a public session option (not deprecated or hidden) is present in the documentation
scanPublicOptions "session"

echo "## Hidden Options"

# Indicates whether a hidden server option is erroneously present in the documentation
scanSomeOptions "server" "isHidden"

# Indicates whether a hidden session option is erroneously present in the documentation. A server option with the same name may be there instead.
scanSomeOptions "session" "isHidden"

echo "## Deprecated Options"

# Indicates whether a deprecated session option is erroneously present in the documentation. A server option with the same name may be there instead.
scanSomeOptions "session" "isDeprecated"

# Indicates whether a deprecated server option is erroneously present in the documentation
scanSomeOptions "server" "isDeprecated"

echo "## Code Options"

# Indicates whether a server option is not being used by the code
scanCodeOptions "server"

# Indicates whether a session option is not being used by the code
scanCodeOptions "session"
