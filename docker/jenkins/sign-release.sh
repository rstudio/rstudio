#!/usr/bin/env bash

if [[ "$#" -lt 3 ]]; then
    echo "Usage: sign-release.sh [installer-file] [key-file] [key-passphrase]"
    exit 1
fi

# label parameters for convenience
INSTALLER=$1
KEYFILE=$2
PASSPHRASE=$3

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
    echo "Signing with debsigs..."
    debsigs -v --sign=origin --default-key=$KEY_ID --secret-keyring=$TMP_KEYRING $INSTALLER

    echo "Signing with dpkg-sig..."
    dpkg-sig -k $KEY_ID --gpgoptions --secret-keyring=$TMP_KEYRING $INSTALLER
elif [ "$EXT" == "rpm" ]; then
    rpmsign -D --addsign $INSTALLER
else
    echo "Unknown extension $EXT."
fi


