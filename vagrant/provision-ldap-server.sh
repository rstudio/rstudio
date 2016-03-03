#!/usr/bin/env bash

# update apt database
apt-get update

# set up config for LDAP ahead of install (otherwise install is interactive)
export DEBIAN_FRONTEND=noninteractive
echo -e " \
slapd    slapd/internal/generated_adminpw    password rsp-pass
slapd    slapd/password2    password rsp-pass
slapd    slapd/internal/adminpw    password rsp-pass
slapd    slapd/password1    password rsp-pass
" | sudo debconf-set-selections
 
# install OpenLDAP
apt-get install -y slapd
apt-get install -y ldap-utils

# populate users
ldapadd -x -D cn=admin,dc=nodomain -w rsp-pass -f /rstudio/vagrant/rstudiousers.ldif

# rewire RStudio Server instances to use newly provisioned server
ssh -i /rstudio/vagrant/vagrant_key_rsa -o StrictHostKeyChecking=no vagrant@192.168.55.101 "sudo /rstudio/vagrant/provision-ldap-client.sh"
ssh -i /rstudio/vagrant/vagrant_key_rsa -o StrictHostKeyChecking=no vagrant@192.168.55.102 "sudo /rstudio/vagrant/provision-ldap-client.sh"

