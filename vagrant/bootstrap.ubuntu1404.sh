# set default route to be the GATEWAY
declare -r GATEWAY=${1:-}
# set default gateway for bridged interface, and make it happen at reboot
if [[ -n "${GATEWAY}" ]]; then
  echo "Setting default gateway through bridged interface"
  ip route delete default 2>&1 >/dev/null || true
  ip route add default via ${GATEWAY}
  echo "GATEWAY=${GATEWAY}" >/etc/default/vagrant-bridge
  [ -r /vagrant/vagrant-bridge.conf ] && cp /vagrant/vagrant-bridge.conf /etc/init/ # symlinks don't work
  initctl reload-configuration
fi

# Fix grub-pc on Virtualbox with Ubuntu
export DEBIAN_FRONTEND=noninteractive

# Standard update + upgrade dance
apt-get update
apt-get upgrade -y

# Fix the silly notion that /bin/sh should point to dash by pointing it to bash

sudo update-alternatives --install /bin/sh sh /bin/bash 100

# Install build tools
apt-get install -y build-essential autoconf automake bison libssl-dev ccache libtool git dkms debhelper emacs libganglia1-dev libapr1-dev libconfuse-dev

# Install other stuff
# apt-get install -y qemu-kvm libvirt-bin ubuntu-vm-builder bridge-utils

# Install uio
apt-get install -y linux-image-extra-`uname -r`

# Install jdk and maven
apt-get install -y openjdk-7-jdk
mkdir /usr/local/apache-maven
cd /usr/local/apache-maven
wget http://apache.go-parts.com/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
tar -xzvf apache-maven-3.3.9-bin.tar.gz -C /usr/local/apache-maven/
update-alternatives --install /usr/bin/mvn mvn /usr/local/apache-maven/apache-maven-3.3.9/bin/mvn 1
update-alternatives --config mvn
cd /home/vagrant
sudo -H -u vagrant mkdir .m2
sudo -H -u vagrant cp /honeycomb/vagrant/settings.xml .m2

cat << EOF > .bash_aliases
export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.9
export MAVEN_OPTS="-Xms256m -Xmx512m" # Very important to put the "m" on the end
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
EOF
chown vagrant:vagrant .bash_aliases

# Use the external Maven M2 repository if it has been mounted on /m2-repository
[ -d /m2-repository ] && sudo -u vagrant sed -i -e 's,/home/vagrant/.m2/repository,/m2-repository,g' /home/vagrant/.m2/settings.xml

# Setup for hugepages using upstart so it persists across reboots
echo "vm.nr_hugepages=1024" >> /etc/sysctl.d/20-hugepages.conf
sysctl --system

cat << EOF > /etc/init/hugepages.conf
start on runlevel [2345]

task

script
    mkdir -p /run/hugepages/kvm || true
    rm -f /run/hugepages/kvm/* || true
    rm -f /dev/shm/* || true
    mount -t hugetlbfs nodev /run/hugepages/kvm
end script
EOF

# Make sure we run that hugepages.conf right now
start hugepages

# Setup the vpp code
cd ~vagrant/

sudo -u vagrant mkdir git
cd git/

# Check if git exists and remove it before attempting clone, else clone ineffective when "reload --provision"
[ -d honeycomb ] && rm -rf honeycomb
sudo -H -u vagrant git clone /honeycomb
cd honeycomb/

# Initial honeycomb build
sudo -H -u vagrant mvn clean install -DskipTests

# Install honeycomb agent (v3po)
mkdir -p /opt/honeycomb/v3po
cp -a v3po/karaf /opt/honeycomb/v3po

# Install ODL Virtual Bridge App
mkdir -p /opt/odl/vbd
cp -a vbd/karaf /opt/odl/vbd

if [ -d /vpp ] ; then
  cd ..

  # Check if git exists and remove it before attempting clone, else clone ineffective when "reload --provision"
  [ -d vpp ] && rm -rf vpp
  sudo -H -u vagrant git clone /vpp
  cd vpp/

  # Initial vpp build
  if [ -d build-root ]; then
    # Bootstrap vpp
    cd build-root/
    sudo -H -u vagrant ./bootstrap.sh

    # Build vpp
    sudo -H -u vagrant make V=0 PLATFORM=vpp TAG=vpp_debug install-deb

    # Install debian packages
    dpkg -i *.deb

    # Disable all ethernet interfaces other than the default route
    # so VPP will use those interfaces.
    for intf in $(ls /sys/class/net) ; do
      if [ -d /sys/class/net/$intf/device ] && [ "$(route | grep default | grep $intf)" == "" ] ; then
        ifconfig $intf down
      fi
    done

    # Start vpp
    start vpp
  fi
fi
