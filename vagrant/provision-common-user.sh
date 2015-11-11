#!/usr/bin/env bash

# add trusted key 
if [ -f /rstudio/vagrant/vagrant_key_rsa.pub ]; then
    cat /rstudio/vagrant/vagrant_key_rsa.pub >> ~/.ssh/authorized_keys
fi

# run overlay script if present
if [ -f ./install-overlay-debian ]; then
    ./install-overlay-debian
fi 

# create build folder and run cmake
mkdir -p /home/vagrant/rstudio-build
cd /home/vagrant/rstudio-build
cmake ~/rstudio/src/cpp -DCMAKE_EXPORT_COMPILE_COMMANDS=1

