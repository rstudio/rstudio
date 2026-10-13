#!/usr/bin/env bash

# turn off posix mode
set +o posix

# reset the ENV environment variable
if [ "${_REALENV}" = "<unset>" ]; then
	unset ENV
else
	ENV="${_REALENV}"
	export ENV
fi

# unset the _REALENV variable
unset _REALENV

# source the system profile
if [ -f /etc/profile ]; then
	source /etc/profile
fi

# source the user startup profile, if any
if [ -f ~/.bash_profile ]; then
	source ~/.bash_profile
elif [ -f ~/.bash_login ]; then
	source ~/.bash_login
elif [ -f ~/.profile ]; then
	source ~/.profile
fi

# the profiles above may have reset PATH (Debian's /etc/profile does); put
# the postback scripts back in front so 'rstudio' resolves to our helper
# rather than an installed application binary
if [ -n "${_RS_POSTBACK_DIR}" ]; then
	case "${PATH}" in
		"${_RS_POSTBACK_DIR}"|"${_RS_POSTBACK_DIR}":*)
			;;
		*)
			PATH="${_RS_POSTBACK_DIR}:${PATH}"
			export PATH
			;;
	esac
	unset _RS_POSTBACK_DIR
fi

# run RStudio terminal hooks
if [ -f "${RSTUDIO_TERMINAL_HOOKS}" ]; then
	source "${RSTUDIO_TERMINAL_HOOKS}"
	unset RSTUDIO_TERMINAL_HOOKS
fi

