#!/usr/bin/env bash

sudo apt-get -y build-dep r-base

# switch to temp dir for download/build
cd /tmp

# download tarballs
wget https://cran.r-project.org/src/base/R-2/R-2.15.3.tar.gz
wget https://cran.r-project.org/src/base/R-3/R-3.0.3.tar.gz

# extract them
tar xzvf R-2.15.3.tar.gz
tar xzvf R-3.0.3.tar.gz

# build and install each version
cd /tmp/R-2.15.3
./configure --prefix=/opt/R/2.15.3 --enable-R-shlib
make 
sudo make install

cd /tmp/R-3.0.3
./configure --prefix=/opt/R/3.0.3 --enable-R-shlib
make 
sudo make install


