#!/usr/bin/env bash

pushd /tmp 

# download the appropriate license server
wget "https://s3.amazonaws.com/rstudio-license-server/rsp-license-server-1.0.0-x86_64.deb"

# install it 
sudo dpkg -i "rsp-license-server-1.0.0-x86_64.deb"

# replace the default configuration file with one that has more development-
# friendly settings
sudo cp /rstudio/vagrant/rsp-license-server.conf /etc/rsp-license-server.conf

# the service needs to be activated before it can be started; run these
# commands after the VM is provisioned:
# 
# sudo service rsp-license-server activate <your-license-key>
# sudo service rsp-license-server start

popd
