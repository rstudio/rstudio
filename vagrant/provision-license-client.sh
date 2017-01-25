#!/usr/bin/env bash

# configure the server to use floating licensing
echo server-license-type=remote >> /etc/rstudio/rserver.conf

# save the license server defined in the Vagrantfile
rstudio-server license-manager license-server 192.168.55.105

