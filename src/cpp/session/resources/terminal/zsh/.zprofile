
# revert back to 'regular' zsh mode
emulate -R zsh

# manually load the bundled zsh modules normally present in a 'regular' zsh instance
zmodload zsh/terminfo
zmodload zsh/zle

# set up relevant options
setopt login
setopt histignorespace

# on macOS, we need to load the 'watch' module to avoid issues with their /zshrc
if [ "$(uname)" = "Darwin" ]; then
	zmodload zsh/watch
fi

# reset the ENV environment variable
if [ "${_REALENV}" = "<unset>" ]; then
	unset ENV
else
	ENV="${_REALENV}"
	export ENV
fi

# unset the _REALENV variable
unset _REALENV

# source startup scripts in the same way a zsh login shell would
# note that different operating systems configure the system-wide
# zsh scripts in different ways.

# zshenv
if [ -f /etc/zsh/zshenv ]; then
	source /etc/zsh/zshenv
elif [ -f /etc/zshenv ]; then
	source /etc/zshenv
fi

if [ -f ~/.zshenv ]; then
	source ~/.zshenv
fi


# zprofile
if [ -f /etc/zsh/zprofile ]; then
	source /etc/zsh/zprofile
elif [ -f /etc/zprofile ]; then
	source /etc/zprofile
fi

if [ -f ~/.zprofile ]; then
	source ~/.zprofile
fi


# zshrc
if [ -f /etc/zsh/zshrc ]; then
	source /etc/zsh/zshrc
elif [ -f /etc/zshrc ]; then
	source /etc/zshrc
fi

if [ -f ~/.zshrc ]; then
	source ~/.zshrc
fi


# zlogin
if [ -f /etc/zsh/zlogin ]; then
	source /etc/zsh/zlogin
elif [ -f /etc/zlogin ]; then
	source /etc/zlogin
fi

if [ -f ~/.zlogin ]; then
	source ~/.zlogin
fi


# run our terminal hooks if available
if [ -f "${RSTUDIO_TERMINAL_HOOKS}" ]; then
	source "${RSTUDIO_TERMINAL_HOOKS}"
	unset RSTUDIO_TERMINAL_HOOKS
fi

