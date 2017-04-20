# create local user accounts
mkdir -p /primary/home
cat << EOF | adduser -f - 
rstudiouser::::::RStudio User:/primary/home/rstudiouser:bash:rstudiouser
rstudiouser2::::::RStudio User 2:/primary/home/rstudiouser2:bash:rstudiouser2
rstudiouser3::::::RStudio User 3:/primary/home/rstudiouser3:bash:rstudiouser3
rstudiouser4::::::RStudio User 4:/primary/home/rstudiouser4:bash:rstudiouser4
rstudiouser5::::::RStudio User 5:/primary/home/rstudiouser5:bash:rstudiouser5
EOF

# enable NFS server and pre-reqs in rc.conf
echo 'nfs_server_enable="YES"' >> /etc/rc.conf
echo 'nfsv4_server_enable="YES"' >> /etc/rc.conf
echo 'nfsuserd_enable="YES"' >> /etc/rc.conf
echo 'nfsuserd_flags="-domain rstudio.nfs4"' >> /etc/rc.conf
echo 'rpcbind_enable="YES"' >> /etc/rc.conf
echo 'nfs_server_flags="-u -t -n 4"' >> /etc/rc.conf
echo 'mountd_flags="-r"' >> /etc/rc.conf
echo 'rpc_lockd_enable="YES"' >> /etc/rc.conf
echo 'rpc_statd_enable="YES"' >> /etc/rc.conf

# set up the exports
echo '/primary/home -network 192.168.55 -mask 255.255.255.0 -alldirs -maproot=root' >> /etc/exports
echo 'V4: /' >> /etc/exports

# run manually: provision the primary machine to use the NFS server
# ssh -i /rstudio/vagrant/vagrant_key_rsa -o StrictHostKeyChecking=no vagrant@192.168.55.101 "sudo /rstudio/vagrant/provision-nfs4-client.sh" 

