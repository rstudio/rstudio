#!/usr/bin/env bash

set -f
USERID=$1
USERINFO=$(getent passwd $USERID)

# okay if no user
if [ $? -ne 0 ]; then
    echo "No user exists with id $USERID"
    exit 0
fi

# turn userinfo into a space-separated array and extract the first element
USERINFO=(${USERINFO//:/ })
USERNAME="${USERINFO[0]}"

echo "Removing user $USERNAME with conflicting id $USERID"
userdel $USERNAME
