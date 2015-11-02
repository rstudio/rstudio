# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ericmann/trusty64"

  # forward typical RStudio server ports from guest to host
  config.vm.network "forwarded_port", guest: 8787, host: 8787, auto_correct: true

  # give machine liberal cpu & core resources for virtualbox (adjust to taste)
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
    vb.cpus = "6"
  end

  # less generous resources on hyper-v
  config.vm.provider "hyperv" do |hv|
    hv.memory = "1024"
    hv.cpus = "2"
  end

  # mount /rstudio and ~/rstudio (for convenience)
  config.vm.synced_folder ".", "/rstudio"
  config.vm.synced_folder ".", "/home/vagrant/rstudio"

  config.vm.provision :shell, path: "vagrant/bootstrap-debian-64.sh", args: 8787
end
