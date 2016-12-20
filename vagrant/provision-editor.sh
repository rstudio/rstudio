# prerequisites for rtags
sudo apt-get install libclang-dev
sudo apt-get install llvm

# install rtags -- provides a server that can be used for code navigation
pushd .
cd /tmp
git clone --recursive https://github.com/Andersbakken/rtags.git
cd rtags
cmake -DCMAKE_EXPORT_COMPILE_COMMANDS=1 .
make
sudo make install
popd

# make rtags socket activated 
mkdir -p ~/.config/systemd/user
cp /rstudio/vagrant/rdm.socket ~/.config/systemd/user
cp /rstudio/vagrant/rdm.service ~/.config/systemd/user
systemctl --user enable rdm.socket
systemctl --user start rdm.socket

# configure a basic c/c++ editing experience inside the VM 
cp /rstudio/vagrant/vimrc /home/vagrant/.vimrc
mkdir -p /home/vagrant/.vimbackup
mkdir -p /home/vagrant/.vimswap
mkdir -p /home/vagrant/.vim/bundle/
git clone https://github.com/VundleVim/Vundle.vim.git /home/vagrant/.vim/bundle/Vundle.vim
cd /home/vagrant
vim -T builtin_dumb +PluginInstall +qall
cd /home/vagrant/.vim/bundle/YouCompleteMe
./install.py --clang-completer

