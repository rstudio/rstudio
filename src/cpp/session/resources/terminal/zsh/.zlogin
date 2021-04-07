
if [ -f "${_ZDOTDIR-$HOME}/.zlogin" ]; then
	
	ZDOTDIR_SAVE="${ZDOTDIR}"
	source "${_ZDOTDIR-$HOME}/.zlogin"

	if [ "${ZDOTDIR}" != "${ZDOTDIR_SAVE}" ]; then
		_ZDOTDIR="${ZDOTDIR}"
	fi

	ZDOTDIR="${ZDOTDIR_SAVE}"
fi

if [ -f "${RSTUDIO_TERMINAL_HOOKS}" ]; then
	source "${RSTUDIO_TERMINAL_HOOKS}"
fi

ZDOTDIR="${_ZDORDIR-$HOME}"

