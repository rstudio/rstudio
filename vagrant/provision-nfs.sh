# create local user accounts
/rstudio/vagrant/provision-create-users.sh

# install NFS server and export user home directories
apt-get update
apt-get install -y nfs-kernel-server
echo "/primary/home   *(rw,sync,no_root_squash)" >> /etc/exports
service nfs-kernel-server start

# replace user home directories on primary machine
ssh -i /rstudio/vagrant/vagrant_key_rsa -o StrictHostKeyChecking=no vagrant@192.168.55.101 "sudo /rstudio/vagrant/provision-nfs-client.sh" 

