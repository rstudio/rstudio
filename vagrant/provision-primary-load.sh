#!/usr/bin/env bash

# copy load balance config script
cp /rstudio/vagrant/load-balancer /etc/rstudio/load-balancer

# copy and set permissions on secure cookie key
cp /rstudio/vagrant/secure-cookie-key /etc/rstudio/secure-cookie-key
chmod 0600 /etc/rstudio/secure-cookie-key

# create shared storage folder and add to config files
mkdir -p /primary/home/shared-storage
chmod 1777 /primary/home/shared-storage
echo "server-shared-storage-path=/primary/home/shared-storage" >> /etc/rstudio/rserver.conf

