#!/usr/bin/env bash

# add trusted key 
if [ -f /rstudio/vagrant/vagrant_key_rsa.pub ]; then
    cat /rstudio/vagrant/vagrant_key_rsa.pub >> ~/.ssh/authorized_keys
fi

# run overlay script if present
if [ -f ./install-overlay-debian ]; then
    ./install-overlay-debian
fi 

