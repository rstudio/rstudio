# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"

  # forward typical RStudio server ports from guest to host
  config.vm.network "forwarded_port", guest: 8787, host: 8787

  # give machine liberal cpu & core resources (adjust to taste)
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
    vb.cpus = "6"
  end

  # hyper-v specific options
  config.vm.provider "hyperv" do |hv|
    config.vm.box = "withinboredom/Trusty64"
  end

  config.vm.provision :shell, path: "vagrant/bootstrap-debian-64.sh"
end
