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

