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
TMP_KEYRING_DIR=$(mktemp -d)
TMP_SEC_KEYRING="$TMP_KEYRING_DIR/secring.gpg"
TMP_PUB_KEYRING="$TMP_KEYRING_DIR/pubring.gpg"

# make sure to clean up the temporary keyring when finished
function cleanup {
    if [ -f "$TMP_SEC_KEYRING_DIR" ]; then 
        rm -rf $TMP_SEC_KEYRING_DIR
    fi
}
trap cleanup EXIT

# import signing key
echo "Installing signing key from $KEYFILE..."
gpg --no-default-keyring --keyring=$TMP_PUB_KEYRING --secret-keyring=$TMP_SEC_KEYRING --import $KEYFILE

echo "Importing passphrase from $PASSFILE..."
PASSPHRASE=$(cat $PASSFILE)

# scrape out the signing key ID
KEY_ID=$(gpg --list-secret-keys --no-default-keyring --keyring=$TMP_PUB_KEYRING --secret-keyring=$TMP_SEC_KEYRING --keyid-format long --with-colons | grep '^sec' | cut --delimiter ':' --fields 5)
echo "Signing installer $INSTALLER with key $KEY_ID..."

# extract filename to infer what kind of archive we're working with
FILENAME=$(basename "$INSTALLER")
EXT=${FILENAME##*.}

if [ "$EXT" == "deb" ]; then
    echo "Signing with debsigs..."
    /usr/bin/expect << EOD
spawn bash -c "debsigs -v --sign=origin --default-key=$KEY_ID --secret-keyring=$TMP_SEC_KEYRING $INSTALLER"
expect "Enter passphrase:"
send "$PASSPHRASE\r"
expect eof
EOD

    echo "Signing with dpkg-sig..."
    dpkg-sig -k $KEY_ID --verbose --sign builder $INSTALLER --gpg-options="--no-default-keyring --secret-keyring=$TMP_SEC_KEYRING --passphrase-file $PASSFILE"
elif [ "$EXT" == "rpm" ]; then
    echo "Signing with rpmsign..."
    
    # set up the rpm macros file to point to our temporary key
    RPM_MACROS="$HOME/.rpmmacros"
    if [ -f "$RPM_MACROS" ]; then
        mv $RPM_MACROS $RPM_MACROS.bak
    fi
    touch "$RPM_MACROS"
    echo "%_signature gpg"  >> $RPM_MACROS
    echo "%_gpg_name $KEY_ID" >> $RPM_MACROS
    echo "%_gpg_path $TMP_KEYRING_DIR" >> $RPM_MACROS
    cat $RPM_MACROS

    # perform the actual signature
    /usr/bin/expect << EOD
spawn bash -c "rpmsign --addsign $INSTALLER"
expect "Enter pass phrase:"
send "$PASSPHRASE\r"
expect eof
EOD

    # restore old rpmacros file if we touched it
    rm -f $RPM_MACROS
    if [ -f "$RPM_MACROS.bak" ]; then
        mv $RPM_MACROS.bak $RPM_MACROS
    fi
else
    echo "Unknown installer extension $EXT."
fi


