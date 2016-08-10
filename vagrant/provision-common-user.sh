#!/usr/bin/env bash

# add trusted key 
if [ -f /rstudio/vagrant/vagrant_key_rsa.pub ]; then
    cat /rstudio/vagrant/vagrant_key_rsa.pub >> ~/.ssh/authorized_keys
fi

# set appropriate permissions on keys
chmod 600 ~/.ssh/authorized_keys

# run overlay script if present
if [ -f ./install-overlay-debian ]; then
    ./install-overlay-debian
fi 

