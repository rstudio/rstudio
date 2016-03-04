#!/usr/bin/env bash

echo "rsp-pass" > /etc/ldap.secret
chmod 0600 /etc/ldap.secret

# set up config for LDAP ahead of install (otherwise install is interactive)
export DEBIAN_FRONTEND=noninteractive
echo -e " \
ldap-auth-config ldap-auth-config/binddn string cn=proxyuser,dc=nodomain
ldap-auth-config ldap-auth-config/ldapns/ldap_version select 3
ldap-auth-config ldap-auth-config/dbrootlogin boolean true
ldap-auth-config ldap-auth-config/rootbinddn string cn=admin,dc=nodomain
ldap-auth-config ldap-auth-config/ldapns/ldap-server string ldapi:///192.168.55.104
ldap-auth-config ldap-auth-config/pam_password select md5
ldap-auth-config ldap-auth-config/move-to-debconf boolean true
ldap-auth-config ldap-auth-config/dblogin boolean false
ldap-auth-config ldap-auth-config/ldapns/base-dn string dc=nodomain
ldap-auth-config ldap-auth-config/override boolean true
" | debconf-set-selections

# install requisite packages
apt-get install -y libnss-ldap
apt-get install -y ldap-utils

# finish up config
auth-client-config -t nss -p lac_ldap
pam-auth-update

