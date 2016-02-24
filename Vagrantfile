# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  # define primary development box
  config.vm.define "primary", primary: true do |p|
    p.vm.box = "ubuntu/trusty64"
    p.vm.network "private_network", ip: "192.168.55.101"
    p.vm.network "forwarded_port", guest: 8787, host: 8787, auto_correct: true
    p.vm.provision :shell, path: "vagrant/bootstrap-debian.sh", args: 8787
    p.vm.provision :shell, path: "vagrant/provision-primary.sh"

    # this machine is used for building; give it liberal resources
    p.vm.provider "virtualbox" do |vb|
      vb.memory = "4096"
      vb.cpus = "4"
    end
  end

  # define secondary box for development of load balanced features. if you want
  # to use this box, make sure you bring up the NFS box (below) first, since 
  # load balancing requires a shared file system.
  config.vm.define "balanced", autostart: false do |b|
    b.vm.box = "ubuntu/trusty64"
    b.vm.network "private_network", ip: "192.168.55.102"
    b.vm.network "forwarded_port", guest: 8787, host: 8788, auto_correct: true
    b.vm.provision :shell, path: "vagrant/bootstrap-debian.sh", args: 8787
    b.vm.provision :shell, path: "vagrant/provision-balancer.sh"

    # primarily used for serving R
    b.vm.provider "virtualbox" do |vb|
      vb.memory = "2048"
      vb.cpus = "2"
    end
  end

  # define NFS box; can be used with or without the load balancer.
  config.vm.define "nfs", autostart: false do |n|
    n.vm.box = "ubuntu/trusty64"
    n.vm.network "private_network", ip: "192.168.55.103"
    n.vm.provision :shell, path: "vagrant/provision-nfs.sh"

    # just a file server
    n.vm.provider "virtualbox" do |vb|
      vb.memory = "1024"
      vb.cpus = "2"
    end
  end

  # less generous resources (and a box that supports hyperv) on hyper-v
  config.vm.provider "hyperv" do |hv|
    hv.vm.box = "ericmann/trusty64"
    hv.memory = "1024"
    hv.cpus = "2"
  end

  config.vm.synced_folder ".", "/rstudio"
  config.vm.synced_folder ".", "/home/vagrant/rstudio"
end
