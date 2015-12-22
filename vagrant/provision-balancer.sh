#!/usr/bin/env bash

# amend MOTD
echo "Load balanced: /etc/rstudio/load-balancer" >> /etc/motd

# set up cluster node config file
cp /rstudio/vagrant/load-balancer /etc/rstudio/load-balancer

# create secure cookie key (keep dev copy accessible)
apt-get install -y uuid
echo `uuid` > /rstudio/vagrant/secure-cookie-key
chown vagrant /rstudio/vagrant/secure-cookie-key
cp /rstudio/vagrant/secure-cookie-key /etc/rstudio/secure-cookie-key
chmod 0600 /etc/rstudio/secure-cookie-key

# configure primary machine for load balancing
ssh -i /rstudio/vagrant/vagrant_key_rsa -o StrictHostKeyChecking=no vagrant@192.168.55.101 "sudo /rstudio/vagrant/provision-primary-load.sh" 

# attach to NFS client
/rstudio/vagrant/provision-nfs-client.sh

# add users; use NFS home directories
apt-get install -y whois
for userdetails in `cat /rstudio/vagrant/rstudiousers.txt`
do
    user=`echo $userdetails | cut -f 1 -d ,`
    passwd=`echo $userdetails | cut -f 2 -d ,`
    useradd --base-dir /primary/home -p `mkpasswd $passwd` $user
done

# configure shared storage
echo "server-shared-storage-path=/primary/home/shared-storage" >> /etc/rstudio/rserver.conf

# TODO Should we route /vagrant's home dir via NFS, too? Needed if we want to 
# be able to have load-balanced sessions as the vagrant user.

# perform remainder of the install script as regular user
sudo --login --set-home -u vagrant /rstudio/vagrant/provision-balancer-user.sh

