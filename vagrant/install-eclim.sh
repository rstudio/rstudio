# install virtual X framebuffer (so we can run eclipse without X)
apt-get install -y xvfb

# download eclipse and eclim
cd /tmp 
wget http://eclipse.bluemix.net/packages/mars.1/data/eclipse-jee-mars-1-linux-gtk-x86_64.tar.gz 
wget http://downloads.sourceforge.net/project/eclim/eclim/2.5.0/eclim_2.5.0.jar

# install eclipse to /opt
cd /opt
tar xzvf /tmp/eclipse-jee-mars-1-linux-gtk-x86_64.tar.gz 
chown -R vagrant /opt/eclipse

# install eclim
sudo -u vagrant java -Dvim.files=/home/vagrant/.vim -Declipse.home=/opt/eclipse -jar /tmp/eclim_2.5.0.jar install



