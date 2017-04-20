# install packages for NFS dependencies
apt-get install -y nfs-common

# install NFSv4 command line tools
apt-get install -y nfs4-acl-tools

# clean up existing user home directories if present
rm -rf /primary/home
mkdir -p /primary/home 

# we won't have a real domain inside the virtual network so agree on a virtual
# domain name
echo 'Domain = rstudio.nfs4' >> /etc/idmapd.conf

# inject mount coummand into fstab and remount
echo "192.168.55.103:/primary/home /primary/home nfs4 rsize=8192,wsize=8192,timeo=14,intr,nfsvers=4,acl" >> /etc/fstab
mount -a 
