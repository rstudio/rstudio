
if [ -f "${_ZDOTDIR-$HOME}/.zlogin" ]; then
	
	ZDOTDIR_SAVE="${ZDOTDIR}"
	source "${_ZDOTDIR-$HOME}/.zlogin"

	if [ "${ZDOTDIR}" != "${ZDOTDIR_SAVE}" ]; then
		_ZDOTDIR="${ZDOTDIR}"
	fi

	ZDOTDIR="${ZDOTDIR_SAVE}"
	unset ZDOTDIR_SAVE
fi

# zsh normally constructs the HISTFILE location from
# ZDOTDIR, so we need to manually restore that to the
# 'correct' value if necessary here
if [ "${HISTFILE}" = "${ZDOTDIR}/.zsh_history" ]; then
	HISTFILE=~/.zsh_history
	export HISTFILE
fi

if [ -f "${RSTUDIO_TERMINAL_HOOKS}" ]; then
	source "${RSTUDIO_TERMINAL_HOOKS}"
fi

ZDOTDIR="${_ZDOTDIR-$HOME}"

