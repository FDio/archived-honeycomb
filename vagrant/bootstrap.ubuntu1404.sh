# Args
VAGRANT_VM_NAME="$1"
VAGRANT_VBD_VM="$2"
VAGRANT_VPP_AGENT_ADDR="$3"
echo "Running bootstrap.ubuntu1404.sh..."
echo "VAGRANT_VM_NAME = '$VAGRANT_VM_NAME'"
echo "VAGRANT_VBD_VM = '$VAGRANT_VBD_VM'"
echo "VAGRANT_VPP_AGENT_ADDR = '$VAGRANT_VPP_AGENT_ADDR'"

# Directory and file definitions
HONEYCOMB_MOUNT="/honeycomb"
KARAF_PACKAGES_MOUNT="/karaf-packages"
M2_SETTINGS_XML="$HONEYCOMB_MOUNT/vagrant/settings.xml"
M2_MOUNT="/m2-repository"
VPP_MOUNT="/vpp"
VPP_BUILD_ROOT="$VPP_MOUNT/build-root"
VAGRANT_HOME="/home/vagrant"
VAGRANT_BASHRC="$VAGRANT_HOME/.bashrc"
VAGRANT_BASH_ALIASES="$VAGRANT_HOME/.bash_aliases"
VAGRANT_M2_DIR="$VAGRANT_HOME/.m2"
VAGRANT_M2_REPOSITORY="$VAGRANT_M2_DIR/repository"
HONEYCOMB_INSTALL_DIR="/opt/honeycomb"
V3PO_TARGET_DIR="$HONEYCOMB_MOUNT/v3po/karaf/target"
V3PO_TARBALL="$KARAF_PACKAGES_MOUNT/v3po-karaf*.tar.gz" 
[ ! -f $V3PO_TARBALL ] && V3PO_TARBALL="$V3PO_TARGET_DIR/v3po-karaf*.tar.gz"
VBD_TARGET_DIR="$HONEYCOMB_MOUNT/vbd/karaf/target"
VBD_TARBALL="$KARAF_PACKAGES_MOUNT/vbd-karaf*.tar.gz" 
[ ! -f $VBD_TARBALL ] && VBD_TARBALL="$VBD_TARGET_DIR/vbd-karaf*.tar.gz"

# Don't install VPP if this is an ODL VBD application VM
if [ "$VAGRANT_VBD_VM" != "is_vbd_vm" ] ; then
  echo "Configuring hugepages for VPP"
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
  # Allocate hugepages.conf right now.  Verify that
  # all hugepages have been allocated.  If the VM
  # runs out of resources, the messages below will
  # identify the issue.  Front and Center!
  nr_hugepages=$(cat /proc/sys/vm/nr_hugepages)
  echo
  while [ $nr_hugepages != 1024 ] ; do
    echo -n "Allocating hugepages... "
    start hugepages
    nr_hugepages=$(cat /proc/sys/vm/nr_hugepages)
    echo "nr_hugepages = $nr_hugepages"
  done
fi

# Set prompt to include VM name if provided.
sudo -H -u vagrant perl -i -pe 's/@\\h/@\\\h\$VM_NAME/g' $VAGRANT_BASHRC
sudo -H -u vagrant touch $VAGRANT_BASH_ALIASES
if [ "$VAGRANT_VM_NAME" != "" ] && [ "$(grep VM_NAME $VAGRANT_BASH_ALIASES)" = "" ] ; then 
    echo -e "\n# Include VM Name in prompt" >> $VAGRANT_BASH_ALIASES
    echo "export VM_NAME=\"($VAGRANT_VM_NAME)\"" >> $VAGRANT_BASH_ALIASES
fi

# Fix grub-pc on Virtualbox with Ubuntu
export DEBIAN_FRONTEND=noninteractive

# Add fd.io apt repo in case its needed
echo "deb http://nexus.fd.io/content/repositories/fd.io.dev/ ./" > /etc/apt/sources.list.d/99fd.io.list

# Standard update + upgrade dance
apt-get update
apt-get upgrade -y

# Install build tools
apt-get install -y build-essential autoconf automake bison libssl-dev ccache libtool git dkms debhelper emacs libganglia1-dev libapr1-dev libconfuse-dev git-review

# Install uio
apt-get install -y linux-image-extra-`uname -r`

# Install jdk and maven
apt-get install -y openjdk-7-jdk
mkdir -p /usr/local/apache-maven
cd /usr/local/apache-maven
wget http://apache.go-parts.com/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
tar -xzvf apache-maven-3.3.9-bin.tar.gz -C /usr/local/apache-maven/
update-alternatives --install /usr/bin/mvn mvn /usr/local/apache-maven/apache-maven-3.3.9/bin/mvn 1
update-alternatives --config mvn

# Set up Maven
sudo -H -u vagrant mkdir -p $VAGRANT_M2_DIR
sudo -H -u vagrant cp $M2_SETTINGS_XML $VAGRANT_M2_DIR
if [ "$(grep M2_HOME $VAGRANT_BASH_ALIASES)" = "" ] ; then
    cat << EOF >> $VAGRANT_BASH_ALIASES

# Maven Environment variables
export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.9
export MAVEN_OPTS="-Xms256m -Xmx512m" # Very important to put the "m" on the end
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
EOF
    chown vagrant:vagrant $VAGRANT_BASH_ALIASES
fi

# Use the external Maven M2 repository as a seed if available.
[ -d /m2-repository ] && sudo -u vagrant sed -i -e 's,/home/vagrant/.m2/repository,/m2-repository,g' /home/vagrant/.m2/settings.xml

# Don't install VPP and ODL Honeycomb Agent if this is an ODL VBD application VM
if [ "$VAGRANT_VBD_VM" != "is_vbd_vm" ] ; then
  # Disable all ethernet interfaces other than the default route
  # interface so VPP will use those interfaces.  The VPP auto-blacklist
  # algorithm prevents the use of any physical interface contained in the
  # routing table (i.e. "route --inet --inet6") preventing the theft of
  # the management ethernet interface by VPP from the kernel.
  for intf in $(ls /sys/class/net) ; do
    if [ "$VAGRANT_VPP_AGENT_ADDR" != "" ] && [ "$(ifconfig $intf | grep $VAGRANT_VPP_AGENT_ADDR)" != "" ] ; then
      continue;
    fi
    if [ -d /sys/class/net/$intf/device ] && [ "$(route --inet --inet6 | grep default | grep $intf)" == "" ] ; then
        ifconfig $intf down
    fi
  done

  # Install VPP
  if [ -d $VPP_MOUNT ] ; then
    # Fix the silly notion that /bin/sh should point to dash by pointing it to bash
    sudo update-alternatives --install /bin/sh sh /bin/bash 100

    # Build and install VPP if necessary
    if [ -d $VPP_BUILD_ROOT ] ; then
      if [ "$(ls $VPP_BUILD_ROOT/*.deb)" = "" ] ; then
        echo "Building VPP"
        # Bootstrap vpp
        cd $VPP_BUILD_ROOT
        sudo -H -u vagrant ./bootstrap.sh
        # Build vpp
        sudo -H -u vagrant make V=0 PLATFORM=vpp TAG=vpp_debug install-deb
      fi
      # Install debian packages
      echo "Installing VPP from $VPP_BUILD_ROOT"
      dpkg -i $VPP_BUILD_ROOT/*.deb
    fi
  else
    echo "Installing VPP from nexus.fd.io"
    apt-get install vpp vpp-dpdk-dev vpp-dpdk-dkms vpp-dev vpp-dbg -y --force-yes
  fi
  # Start VPP if it is installed.
  if [ "$(dpkg -l | grep vpp)" != "" ] ; then
    echo "Starting VPP"
    start vpp
  fi

  # Build Honeycomb if necessary
  if [ ! -f $V3PO_TARBALL ] ; then
    echo "Building Honeycomb..."
    cd $HONEYCOMB_MOUNT
    sudo -H -u vagrant mvn clean install -DskipTests
  fi

  # Install honeycomb agent (v3po) if available.
  if [ -f $V3PO_TARBALL ] ; then
    echo
    echo "Installing Honeycomb VPP agent in $HONEYCOMB_INSTALL_DIR/$V3PO_SNAPSHOT"
    [ ! -d $HONEYCOMB_INSTALL_DIR ] && mkdir -p $HONEYCOMB_INSTALL_DIR
    cd $HONEYCOMB_INSTALL_DIR
    [ -d $V3PO_SNAPSHOT ] && rm -rf $V3PO_SNAPSHOT
    V3PO_KARAF_DIR="$HONEYCOMB_INSTALL_DIR/$(basename $V3PO_TARBALL|perl -pe 's/\.tar.gz//g')"
    [ -d $V3PO_KARAF_DIR ] && rm -rf $V3PO_KARAF_DIR
    tar xzf $V3PO_TARBALL
    $V3PO_KARAF_DIR/bin/start
  fi
else
  # Build Honeycomb if necessary
  if [ ! -f $VBD_TARBALL ] ; then
    echo "Building Honeycomb..."
    cd $HONEYCOMB_MOUNT
    sudo -H -u vagrant mvn clean install -DskipTests
  fi
  # Install ODL Virtual Bridge Domain App if available.
  if [ -f $VBD_TARBALL ] ; then
    echo
    echo "Installing ODL Virtual Bridge Domain application in $HONEYCOMB_INSTALL_DIR/$VBD_SNAPSHOT"
    [ ! -d $HONEYCOMB_INSTALL_DIR ] && mkdir -p $HONEYCOMB_INSTALL_DIR
    cd $HONEYCOMB_INSTALL_DIR
    VBD_KARAF_DIR="$HONEYCOMB_INSTALL_DIR/$(basename $VBD_TARBALL|perl -pe 's/\.tar.gz//g')"
    [ -d $VBD_KARAF_DIR ] && rm -rf $VBD_KARAF_DIR
    tar xzf $VBD_TARBALL
    $VBD_KARAF_DIR/bin/start
  fi
fi

echo "VM Installation Complete!"
echo
