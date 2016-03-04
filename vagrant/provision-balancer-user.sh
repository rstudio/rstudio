#!/usr/bin/env bash

# install dependencies (we know this is a server so no need to include Qt)
cd /rstudio/dependencies/linux
./install-dependencies-debian --exclude-qt-sdk

# resiliency (in case the above aborts early)
./install-dependencies-debian --exclude-qt-sdk

# run common user provisioning script
cd /rstudio/vagrant
./provision-common-user.sh

# create build folder and run cmake
mkdir -p /home/vagrant/rstudio-build
cd /home/vagrant/rstudio-build
cmake ~/rstudio/src/cpp -DRSTUDIO_TARGET=Server -DCMAKE_EXPORT_COMPILE_COMMANDS=1

