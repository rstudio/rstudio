#!/usr/bin/env bash

# install dependencies
export QT_SDK_DIR=/home/vagrant/Qt5.4.0
cd /rstudio/dependencies/linux

LINUX_SYS=debian
if [ -f /etc/redhat-release ]; then
    LINUX_SYS=yum
fi

./install-dependencies-$LINUX_SYS

# resiliency
./install-dependencies-$LINUX_SYS

# run overlay script if present
if [ -f ./install-overlay-$LINUX_SYS ]; then
    ./install-overlay-$LINUX_SYS
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

# add compile commands to rdm index 
rcpath=$(which rc)
if [ -x "$rcpath" ]; then
   $rcpath -J /home/vagrant/rstudio-build
fi

# perform overlay config
./provision-primary-overlay.sh

