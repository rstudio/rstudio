#!/usr/bin/env bash

# set motd
cp /vagrant/vagrant/build.motd.tail /etc/motd

# add port information to the motd
echo "RStudio server port: $1" >> /etc/motd

# add repo for R 
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E084DAB9
echo "deb https://cran.rstudio.com/bin/linux/ubuntu trusty/" >> /etc/apt/sources.list

# bring apt database up to date with R packages
apt-get update

# install R
apt-get install -y --force-yes r-base r-base-dev

# install minimal packages needed to run bootstrap scripts
apt-get install -y unzip
apt-get install -y git
apt-get install -y g++

# add users 
apt-get install -y whois
for userdetails in `cat /vagrant/vagrant/rstudiousers.txt`
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
sudo -u vagrant /vagrant/vagrant/bootstrap-user.sh

