#!/usr/bin/env bash

# copy load balance config script
cp /rstudio/vagrant/load-balancer /etc/rstudio/load-balancer

# copy and set permissions on secure cookie key
cp /rstudio/vagrant/secure-cookie-key /etc/rstudio/secure-cookie-key
chmod 0600 /etc/rstudio/secure-cookie-key

# install NFS server and export user home directories
apt-get install -y nfs-kernel-server
echo "/primary/home   *(rw,sync,no_root_squash)" >> /etc/exports
service nfs-kernel-server start

# create shared storage folder and add to config files
mkdir -p /home/shared-storage
chmod 1777 /home/shared-storage
echo "server-shared-storage-path=/home/shared-storage" >> /etc/rstudio/rserver.conf

