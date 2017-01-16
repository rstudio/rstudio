#!/usr/bin/env bash

pushd /tmp 

# download the appropriate license server
wget "https://s3.amazonaws.com/rstudio-license-server/rsp-license-server-1.0.0-x86_64.deb"

# install it 
sudo dpkg -i "rsp-license-server-1.0.0-x86_64.deb"

# start it!
service rsp-license-server start

popd
