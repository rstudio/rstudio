
if [ -f "${_ZDOTDIR-$HOME}/.zlogin" ]; then
	
	ZDOTDIR_SAVE="${ZDOTDIR}"
	source "${_ZDOTDIR-$HOME}/.zlogin"

	if [ "${ZDOTDIR}" != "${ZDOTDIR_SAVE}" ]; then
		_ZDOTDIR="${ZDOTDIR}"
	fi

	ZDOTDIR="${ZDOTDIR_SAVE}"
fi

if [ -n "${RETICULATE_PYTHON}" ]; then
	PATH="$(dirname "${RETICULATE_PYTHON}"):${PATH}"
fi

ZDOTDIR="${_ZDORDIR-$HOME}"

