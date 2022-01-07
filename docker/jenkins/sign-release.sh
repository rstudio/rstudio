#!/usr/bin/env bash
#
# RStudio Release Signing (sign-release.sh)
#
# This script signs an RStudio release using GnuPG and a private RStudio
# release signing key. The command-line parameters are as follows:
#
# 1. The release binary. This can be an RPM, which will be signed with rpmsign,
#    or a DEB, which will be signed with dpkg-sig.
#
# 2. The encrypted private release signing key. This should be an ASCII armored
#    GnuPG private key, via e.g. gpg --export-secret-keys. 
# 
# 3. A file containing the passphrase for the release signing key.
#
# The script will not modify the GnuPG keyring of the calling user; it imports
# the signing key into a private, temporary keyring, uses it to sign the
# release, and then destroys the keyring.

# abort on error
set -e

if [[ "$#" -lt 2 ]]; then
    echo "Usage: sign-release.sh [installer-file] [key-file] [passphrase-file]"
    exit 1
fi

# label parameters for convenience
INSTALLER=$1
KEYFILE=$2
PASSFILE=$3

# to avoid cluttering the user's keyring with the signing key, we use a
# temporary keyring directory
TMP_KEYRING_DIR=$(mktemp -d)
TMP_SEC_KEYRING="$TMP_KEYRING_DIR/secring.gpg"
TMP_PUB_KEYRING="$TMP_KEYRING_DIR/pubring.gpg"

# make sure to clean up the temporary keyring when finished
function cleanup {
    if [ -d "$TMP_KEYRING_DIR" ]; then 
        rm -rf $TMP_KEYRING_DIR
    fi
}
trap cleanup EXIT

# this script only works with GnuPG 1.4 and 2.0; in 2.1 all the secret
# management happens in gpg-agent, which is much less amenable to automation.
# if the gpg1 binary is present on this machine, then presume that it exists as
# an alternative to a future, incompatible version, and use it for the "gpg"
# command by manipulating $PATH.
if [ -x "$(command -v gpg1)" ]; then 
    # emit notice
    GPG1=$(which gpg1)
    GPG2=$(which gpg)
    echo "Note: Using $GPG1 to provide GnuPG (was $GPG2)"
    # softlink gpg into our temporary work folder
    ln -s $GPG1 $TMP_KEYRING_DIR/gpg
    export PATH=$TMP_KEYRING_DIR:$PATH
fi

echo "Using GPG version: "
gpg --version

# import signing key
echo "Installing signing key from $KEYFILE..."
gpg --no-default-keyring --keyring=$TMP_PUB_KEYRING --secret-keyring=$TMP_SEC_KEYRING --import $KEYFILE
PASSPHRASE=$(cat $PASSFILE)

# scrape out the signing key ID
KEY_ID=$(gpg --list-secret-keys --no-default-keyring --keyring=$TMP_PUB_KEYRING --secret-keyring=$TMP_SEC_KEYRING --keyid-format long --with-colons | grep '^sec' | cut --delimiter ':' --fields 5)
echo "Signing installer $INSTALLER with key $KEY_ID..."

# extract filename to infer what kind of archive we're working with
FILENAME=$(basename "$INSTALLER")
EXT=${FILENAME##*.}

if [ "$EXT" == "deb" ]; then
    # ------------------------------------------------------------------------
    # Debian packages (.deb)
    # ------------------------------------------------------------------------

    # Signing with debsigs is currently disabled because it doesn't provide a
    # way to pass the *public* keyring to GPG. Even when passing the correct *secret*
    # keyring, GPG doesn't sign:
    # 
    # secret key without public key - skipped
    # gpg: no default secret key: secret key not available
    #
    # This seems to be a problem primarily with older GPG installations.

    # echo "Signing with debsigs..."
    # /usr/bin/expect << EOD
    # spawn bash -c "debsigs -v --sign=origin --default-key=$KEY_ID --secret-keyring=$TMP_SEC_KEYRING $INSTALLER"
    # expect "Enter passphrase:"
    # send "$PASSPHRASE\r"
    # expect eof
    # EOD

    echo "Signing with dpkg-sig..."
    dpkg-sig -k $KEY_ID --verbose --sign builder $INSTALLER --gpg-options="--no-default-keyring --keyring=$TMP_PUB_KEYRING --secret-keyring=$TMP_SEC_KEYRING --no-tty --no-use-agent --passphrase-file $PASSFILE"
elif [ "$EXT" == "rpm" ]; then
    # ------------------------------------------------------------------------
    # Redhat packages (.rpm)
    # ------------------------------------------------------------------------
    echo "Signing with rpmsign..."
    
    # set up the rpm macros file to point to our temporary key
    RPM_MACROS="$HOME/.rpmmacros"
    if [ -f "$RPM_MACROS" ]; then
        mv $RPM_MACROS $RPM_MACROS.bak
    fi
    touch "$RPM_MACROS"
    echo "%_signature gpg"  >> $RPM_MACROS

    # define key name and directory where we installed temporaries
    echo "%_gpg_name $KEY_ID" >> $RPM_MACROS
    echo "%_gpg_path $TMP_KEYRING_DIR" >> $RPM_MACROS

    if [ -f /etc/redhat-release ]; then
       REDHAT_VERSION=$(cat /etc/redhat-release | grep -oP "CentOS Linux release \K[\w.]+") || true
       VERSION_ARRAY=(${REDHAT_VERSION//./ })
       if [ -z ${REDHAT_VERSION} ]; then
          FORCE_NO_EXPECT=true
          echo "Not using expect approach because we are on a newer version of Redhat"
       fi
    fi

    if [ -f /etc/fedora-release ] || [ "$FORCE_NO_EXPECT" = true ]; then
        # on fedora and centos 8 and greater, the expect-based approach doesn't work, so attempt to
        # supply the passphrase by redefining the GPG signature command in the
        # RPM macros definition to take a passphrase-file.
        echo "%__gpg_sign_cmd %{__gpg} \\" >> $RPM_MACROS
        echo "    gpg --no-verbose --no-armor --batch --pinentry-mode loopback \\" >> $RPM_MACROS
        echo "    --passphrase-file $PASSFILE \\" >> $RPM_MACROS
        echo "    %{?_gpg_sign_cmd_extra_args:%{_gpg_sign_cmd_extra_args}} \\" >> $RPM_MACROS
        echo "    %{?_gpg_digest_algo:--digest-algo %{_gpg_digest_algo}} \\" >> $RPM_MACROS
        echo "    --no-secmem-warning \\" >> $RPM_MACROS
        echo "    -u "%{_gpg_name}" -sbo %{__signature_filename} %{__plaintext_filename}" >> $RPM_MACROS

        rpmsign --addsign $INSTALLER
    else
        # on CentOS (and other RedHat platforms), use expect to supply the
        # passphrase "manually"
        /usr/bin/expect << EOD
spawn bash -c "rpmsign --addsign $INSTALLER"
expect "Enter pass phrase:"
send "$PASSPHRASE\r"
expect eof
EOD
    fi

    # dump the contents of the RPM macro files to stdout so that they're
    # visible in the build logs (this helps diagnose signing issues)
    cat $RPM_MACROS

    # restore old rpmacros file if we touched it
    rm -f $RPM_MACROS
    if [ -f "$RPM_MACROS.bak" ]; then
        mv $RPM_MACROS.bak $RPM_MACROS
    fi
else
    # not a deb or rpm; we don't know how to sign this
    echo "Unknown installer extension $EXT."
fi
