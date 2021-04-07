#!/usr/bin/env bash

if [ -f /etc/profile ]; then
	source /etc/profile
fi

if [ -f ~/.bash_profile ]; then
	source ~/.bash_profile
elif [ -f ~/.bash_login ]; then
	source ~/.bash_login
elif [ -f ~/.profile ]; then
	source ~/.profile
fi

# if RETICULATE_PYTHON is set, then place that
# first on the PATH so that a user invocation of
# 'python' would invoke the RStudio-configured
# version of Python
if [ -n "${RETICULATE_PYTHON}" ]; then
	PATH="$(dirname "${RETICULATE_PYTHON}"):${PATH}"
fi
