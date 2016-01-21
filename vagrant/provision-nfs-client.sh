# install packages for NFS dependencies
apt-get install -y nfs-common

# clean up existing user home directories if present
rm -rf /primary/home
mkdir -p /primary/home 

# inject mount coummand into fstab and remount
echo "192.168.55.103:/primary/home /primary/home nfs rsize=8192,wsize=8192,timeo=14,intr,nfsvers=3,acl" >> /etc/fstab
mount -a 
