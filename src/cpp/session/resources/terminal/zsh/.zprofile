
if [ -f "${_ZDOTDIR-$HOME}/.zprofile" ]; then
	
	ZDOTDIR_SAVE="${ZDOTDIR}"
	source "${_ZDOTDIR-$HOME}/.zprofile"

	if [ "${ZDOTDIR}" != "${ZDOTDIR_SAVE}" ]; then
		_ZDOTDIR="${ZDOTDIR}"
	fi

	ZDOTDIR="${ZDOTDIR_SAVE}"
fi

