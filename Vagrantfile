# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  # define primary development box
  config.vm.define "primary", primary: true do |p|
    p.vm.box = "ubuntu/trusty64"
    p.vm.network "private_network", ip: "192.168.55.101"
    p.vm.network "forwarded_port", guest: 8787, host: 8787, auto_correct: true
    p.vm.provision :shell, path: "vagrant/provision-primary.sh"
  end

  # define secondary box for development of load balanced features -- not 
  # started by default; use 'vagrant up balanced' to start it (this will also
  # configure the primary box to participate in the load balancing)
  config.vm.define "balanced", autostart: false do |b|
    b.vm.box = "ubuntu/trusty64"
    b.vm.network "private_network", ip: "192.168.55.102"
    b.vm.network "forwarded_port", guest: 8787, host: 8788, auto_correct: true
    b.vm.provision :shell, path: "vagrant/provision-balancer.sh"
  end

  # give machine liberal cpu & core resources for virtualbox (adjust to taste)
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
    vb.cpus = "4"
  end

  # less generous resources (and a box that supports hyperv) on hyper-v
  config.vm.provider "hyperv" do |hv|
    hv.vm.box = "ericmann/trusty64"
    hv.memory = "1024"
    hv.cpus = "2"
  end

  # mount /rstudio and ~/rstudio (for convenience)
  config.vm.synced_folder ".", "/rstudio"
  config.vm.synced_folder ".", "/home/vagrant/rstudio"

  config.vm.provision :shell, path: "vagrant/bootstrap-debian.sh", args: 8787
end
