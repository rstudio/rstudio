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

# install dependencies
export QT_SDK_DIR=/home/vagrant/Qt5.4.0
cd /vagrant/dependencies/linux
sudo -u vagrant ./install-dependencies-debian

# resiliency (in case the above aborts early)
sudo -u vagrant ./install-dependencies-debian

# run overlay script if present
if [ -f ./install-overlay-debian ]; then
    sudo -u vagrant ./install-overlay-debian
fi 

# create build folder and run cmake
sudo -u vagrant mkdir -p /home/vagrant/rstudio-build
pushd .
cd /home/vagrant/rstudio-build
sudo -u vagrant cmake /vagrant/src/cpp
popd 


