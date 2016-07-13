# add users (create home directories)
mkdir -p /primary/home
if [ -f /etc/redhat-release ]; then
    # TODO: mkpassword from 'expect' doesn't actually generate hashed passwords; 
    # find a utility that does
    yum install -y expect
else
    apt-get install -y whois
fi
for userdetails in `cat /rstudio/vagrant/rstudiousers.txt`
do
    user=`echo $userdetails | cut -f 1 -d ,`
    passwd=`echo $userdetails | cut -f 2 -d ,`
    useradd --base-dir /primary/home --create-home -p `mkpasswd $passwd` $user
done

