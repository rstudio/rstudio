#!/bin/bash -e

DIRNAME=`dirname $0`
cd $DIRNAME
USAGE="$0 [ --update ] [ --rpm ]"

if ! [ -x /usr/sbin/semodule ]; then
  echo "Unable to find /usr/sbin/semodule"
  echo
  echo "Please install the SELinux policy development tools."
  echo "This may be found in your distro's selinux-policy-devel"
  echo "or selinux-policy-dev package."
  exit 1
fi

UPDATE=0
BUILD_MAN=0
BUILD_RPM=0
INSTALL=0
SHOW_USAGE=0

while test $# -gt 0; do
	case "$1" in
		--update)
			UPDATE=1
			;;
		--rpm)
			BUILD_MAN=1
			BUILD_RPM=1
			;;
		--man)
			BUILD_MAN=1
			;;
		--install)
			INSTALL=1
			;;
		*)
			SHOW_USAGE=1
			;;
	esac
	shift
done

if [[ "$BUILD_RPM" == "1" ]] && command -v rpmbuild > /dev/null; then
  echo "Unable to find rpmbuild"
  echo
  echo "Please install the rpmdevtools package."
  exit 1
fi

if [[ "$SHOW_USAGE" == "1" ]]; then
	echo -e $USAGE
	exit 1
fi

if [[ "$UPDATE" == "1" ]] ; then
	if [ `id -u` != 0 ]; then
		echo "Updating the SELinux policy requires root access."
		exit 1
	fi

	time=`ls -l --time-style="+%x %X" rstudio.te | awk '{ printf "%s %s", $6, $7 }'`
	rules=`ausearch --start $time -m avc --raw -se rstudio`
	if [ x"$rules" != "x" ] ; then
		echo "Found avc's to update policy with"
		echo -e "$rules" | audit2allow -R
		echo "Do you want these changes added to policy [y/n]?"
		read ANS
		if [ "$ANS" = "y" -o "$ANS" = "Y" ] ; then
			echo "Updating policy"
			echo -e "$rules" | audit2allow -R >> rstudio.te
			# Fall though and rebuild policy
		else
			exit 0
		fi
	else
		echo "No new avcs found"
		exit 0
	fi
fi

echo "Building policy..."
set -x
make -f /usr/share/selinux/devel/Makefile rstudio.pp || exit

if [[ "$BUILD_MAN" == "1" ]]; then
	# Generate a man page of the installed module
	sepolicy manpage -p . -d rstudio_t rstudio_server_t
fi

if [[ "$INSTALL" == "1" ]]; then
	if [ `id -u` != 0 ]; then
		echo "Installing the SELinux policy requires root access."
		exit 1
	fi

	# install the policy into the running system
	/usr/sbin/semodule -i rstudio.pp

	# Update the file contexts
	for PATH in $(cut -f1 -d' ' rstudio.fc); do
		/sbin/restorecon -F -R -v $PATH
	done
fi

if [[ "$BUILD_RPM" == "1" ]]; then
	# Generate a rpm package for the newly generated policy
	pwd=$(pwd)
	rpmbuild --define "_sourcedir ${pwd}" --define "_specdir ${pwd}" --define "_builddir ${pwd}" --define "_srcrpmdir ${pwd}" --define "_rpmdir ${pwd}" --define "_buildrootdir ${pwd}/.build" -ba rstudio_selinux.spec
fi
