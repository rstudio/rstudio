#!/usr/bin/env bash

# add users (create home directories)
apt-get install -y whois
for userdetails in `cat /rstudio/vagrant/rstudiousers.txt`
do
    user=`echo $userdetails | cut -f 1 -d ,`
    passwd=`echo $userdetails | cut -f 2 -d ,`
    useradd --create-home -p `mkpasswd $passwd` $user
done

# install packages needed for development environment
apt-get install -y vim
apt-get install -y silversearcher-ag
apt-get install -y python-dev

# perform remainder of the install script as regular user
sudo --login --set-home -u vagrant /rstudio/vagrant/provision-primary-user.sh

