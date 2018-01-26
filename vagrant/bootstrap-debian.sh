#!/usr/bin/env bash

# set motd
cp /rstudio/vagrant/build.motd.tail /etc/motd

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
apt-get install -y wget

# install packages needed to build and run devtools
apt-get install -y libssh2-1-dev
apt-get install -y curl 
apt-get install -y libcurl4-openssl-dev

# install GNU debugger
apt-get install -y gdb

# add ppa repository so we can install java 8 (not in any official repo for trusty)
apt-get install -y software-properties-common python-software-properties
add-apt-repository -y ppa:openjdk-r/ppa 
apt-get update
apt-get install -y openjdk-8-jdk

# create SSH key if needed
if [ ! -f /rstudio/vagrant/vagrant_key_rsa ]; then
    ssh-keygen -t rsa -b 4096 -N "" -C "vagrant ssh" -f /rstudio/vagrant/vagrant_key_rsa
    chmod 0600 /rstudio/vagrant/vagrant_key_rsa.pub
fi

# download and expand pre-built Boost libraries (TODO: should verify that we're
# on a compatible system) 
mkdir -p /opt/rstudio-tools
cd /opt/rstudio-tools
wget https://s3.amazonaws.com/rstudio-buildtools/rstudio-boost/boost-linux-gcc48-x86_64.tar.gz
tar xzvf boost-linux-gcc48-x86_64.tar.gz
rm boost-linux-gcc48-x86_64.tar.gz

# create RStudio config directory
mkdir -p /etc/rstudio

