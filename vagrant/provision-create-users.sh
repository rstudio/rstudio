# add users (create home directories)
mkdir -p /primary/home
apt-get install -y whois
for userdetails in `cat /rstudio/vagrant/rstudiousers.txt`
do
    user=`echo $userdetails | cut -f 1 -d ,`
    passwd=`echo $userdetails | cut -f 2 -d ,`
    useradd --base-dir /primary/home --create-home -p `mkpasswd $passwd` $user
done

