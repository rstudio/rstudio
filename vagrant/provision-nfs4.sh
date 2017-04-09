# create local user accounts
mkdir -p /primary/home
adduser -f /rstudio/vagrant/rstudiousers.bsd

# enable NFS server and pre-reqs in rc.conf
echo 'nfs_server_enable="YES"' >> /etc/rc.conf
echo 'rpcbind_enable="YES"' >> /etc/rc.conf
echo 'nfs_server_flags="-u -t -n 4"' >> /etc/rc.conf
echo 'mountd_flags="-r"' >> /etc/rc.conf
echo 'rpc_lockd_enable="YES"' >> /etc/rc.conf
echo 'rpc_statd_enable="YES"' >> /etc/rc.conf

# set up the exports
echo '/primary/home' >> /etc/exports

# start the server right away
rpcbind
nfsd -u -t -n 4
mountd -r

# provision the primary machine
ssh -i /rstudio/vagrant/vagrant_key_rsa -o StrictHostKeyChecking=no vagrant@192.168.55.101 "sudo /rstudio/vagrant/provision-nfs4-client.sh" 

