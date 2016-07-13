#!/usr/bin/env bash

# install packages needed for development environment
if [ ! -f /etc/redhat-release ]; then
    # create local user accounts
    /rstudio/vagrant/provision-create-users.sh

    apt-get install -y vim
    apt-get install -y silversearcher-ag
    apt-get install -y python-dev

    # install NPM and utilities for JavaScript development
    apt-get install -y npm
    update-alternatives --install /usr/bin/node node /usr/bin/nodejs 10
    npm install -g tern
    npm install -g jshint 
    npm install -g grunt-cli
fi

# perform remainder of the install script as regular user
sudo --login --set-home -u vagrant /rstudio/vagrant/provision-primary-user.sh

