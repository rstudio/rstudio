#!/usr/bin/env bash

if [[ "$#" -lt 2 ]]; then
    echo "Usage: sign-release.sh [installer-file] [key-file] [passphrase-file]"
    exit 1
fi

# label parameters for convenience
INSTALLER=$1
KEYFILE=$2
PASSFILE=$3

# to avoid cluttering the user's keyring with the signing key, we use a
# temporary secret keyring
TMP_KEYRING=$(mktemp)

# make sure to clean up the temporary keyring when finished
function cleanup {
    if [ -f "$TMP_KEYRING" ]; then 
        rm -f $TMP_KEYRING
    fi
}
trap cleanup EXIT

# import signing key
echo "Installing signing key from $KEYFILE..."
gpg --no-default-keyring --secret-keyring=$TMP_KEYRING --import $KEYFILE

# scrape out the signing key ID
KEY_ID=$(gpg --list-secret-keys --no-default-keyring --secret-keyring=$TMP_KEYRING --keyid-format long --with-colons | grep '^sec' | cut --delimiter ':' --fields 5)
echo "Signing installer $INSTALLER with key $KEY_ID..."

# extract filename to infer what kind of archive we're working with
FILENAME=$(basename "$INSTALLER")
EXT=${FILENAME##*.}

if [ "$EXT" == "deb" ]; then
    # TODO: debsigs doesn't currently have a way to manipulate the gpg options
    # such that it's possible to feed in the passphrase non-interactively
    # (might have to do something with gpg-agent to make this possible)
    #
    # echo "Signing with debsigs..."
    # debsigs -v --sign=origin --default-key=$KEY_ID --secret-keyring=$TMP_KEYRING $INSTALLER

    echo "Signing with dpkg-sig..."
    dpkg-sig -k $KEY_ID --verbose --sign builder $INSTALLER --gpg-options="--no-default-keyring --secret-keyring=$TMP_KEYRING --passphrase-file $PASSFILE"
elif [ "$EXT" == "rpm" ]; then
    echo "Signing with rpmsign..."
    
    # set up the rpm macros file to point to our temporary key
    RPM_MACROS="~/.rpmmacros"
    if [ -f "$RPM_MACROS" ]; then
        mv $RPM_MACROS $RPM_MACROS.bak
    fi
    echo "%_signature gpg"  >> $RPM_MACROS
    echo "%_gpg_name $KEY_ID" >> $RPM_MACROS
    echo "%_gpg_path $TMP_KEYRING" >> $RPM_MACROS

    # perform the actual signature
    rpmsign -D --addsign $INSTALLER

    # restore old rpmacros file if we touched it
    rm -f $RPM_MACROS
    if [ -f "$RPM_MACROS.bak" ]; then
        mv $RPM_MACROS.bak $RPM_MACROS
    fi
else
    echo "Unknown installer extension $EXT."
fi


