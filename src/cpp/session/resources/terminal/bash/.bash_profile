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

# the profiles above may have reset PATH (Debian's /etc/profile does) or
# moved the postback scripts dir to the end (macOS path_helper does); drop any
# existing copies and put it back in front so 'rstudio' resolves to our helper
# rather than an installed application binary
if [ -n "${_RS_POSTBACK_DIR}" ]; then
	_rs_path=":${PATH}:"
	_rs_stripped="${_rs_path//":${_RS_POSTBACK_DIR}:"/:}"
	while [ "${_rs_stripped}" != "${_rs_path}" ]; do
		_rs_path="${_rs_stripped}"
		_rs_stripped="${_rs_path//":${_RS_POSTBACK_DIR}:"/:}"
	done
	_rs_path="${_rs_path#:}"
	_rs_path="${_rs_path%:}"
	PATH="${_RS_POSTBACK_DIR}${_rs_path:+:${_rs_path}}"
	export PATH
	unset _rs_path _rs_stripped _RS_POSTBACK_DIR
fi

# run RStudio terminal hooks
if [ -f "${RSTUDIO_TERMINAL_HOOKS}" ]; then
	source "${RSTUDIO_TERMINAL_HOOKS}"
	unset RSTUDIO_TERMINAL_HOOKS
fi

if [ -n "${RSTUDIO_POSTBACK_SCRIPTS_DIR:-}" ]; then
	case "${PATH:-}" in
		"${RSTUDIO_POSTBACK_SCRIPTS_DIR}"|"${RSTUDIO_POSTBACK_SCRIPTS_DIR}":*) ;;
		*) PATH="${RSTUDIO_POSTBACK_SCRIPTS_DIR}:${PATH:-}" ;;
	esac
	export PATH
	unset RSTUDIO_POSTBACK_SCRIPTS_DIR
fi

