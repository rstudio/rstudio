#!/usr/bin/env bash

# install dependencies
export QT_SDK_DIR=/home/vagrant/Qt5.4.0
cd /rstudio/dependencies/linux
./install-dependencies-debian

# resiliency (in case the above aborts early)
./install-dependencies-debian

# run overlay script if present
if [ -f ./install-overlay-debian ]; then
    ./install-overlay-debian
fi 


cd /rstudio/vagrant

# configure a basic c/c++ editing experience inside the VM 
./provision-editor.sh

# run common user provisioning script
./provision-common-user.sh

# create build folder and run cmake
mkdir -p /home/vagrant/rstudio-build
cd /home/vagrant/rstudio-build
cmake ~/rstudio/src/cpp -DCMAKE_EXPORT_COMPILE_COMMANDS=1

