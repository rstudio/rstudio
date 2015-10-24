# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ericmann/trusty64"

  # select a random port so multiple guest servers can exist on a single host
  port = rand(30000) + 1024

  # forward typical RStudio server ports from guest to host
  config.vm.network "forwarded_port", guest: 8787, host: port

  # give machine liberal cpu & core resources for virtualbox (adjust to taste)
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
    vb.cpus = "6"
  end

  config.vm.provision :shell, path: "vagrant/bootstrap-debian-64.sh", args: port
end
