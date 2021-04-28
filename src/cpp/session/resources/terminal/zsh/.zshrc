
if [ -f "${_ZDOTDIR-$HOME}/.zshrc" ]; then
	
	ZDOTDIR_SAVE="${ZDOTDIR}"
	source "${_ZDOTDIR-$HOME}/.zshrc"

	if [ "${ZDOTDIR}" != "${ZDOTDIR_SAVE}" ]; then
		_ZDOTDIR="${ZDOTDIR}"
	fi

	ZDOTDIR="${ZDOTDIR_SAVE}"
fi

