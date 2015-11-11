#!/usr/bin/env bash

# install dependencies (we know this is a server so no need to include Qt)
cd /rstudio/dependencies/linux
./install-dependencies-debian --exclude-qt-sdk

# resiliency (in case the above aborts early)
./install-dependencies-debian --exclude-qt-sdk

# run common user provisioning script
cd /rstudio/vagrant
./provision-common-user.sh

