#!/usr/bin/env bash

# set HOME directory
_FAKEHOME="${HOME}"
HOME="${_REALHOME}"

# source the user startup file, if any
if [ -f ~/.bash_profile ]; then
	source ~/.bash_profile
elif [ -f ~/.bash_login ]; then
	source ~/.bash_login
elif [ -f ~/.profile ]; then
	source ~/.profile
fi

# set HISTFILE if necessary
if [ "${HISTFILE}" = "${_FAKEHOME}/.bash_history" ]; then
	HISTFILE=~/.bash_history
fi

# run RStudio terminal hooks
if [ -f "${RSTUDIO_TERMINAL_HOOKS}" ]; then
	source "${RSTUDIO_TERMINAL_HOOKS}"
fi

# clean up our variables
unset _FAKEHOME

