# Args
VAGRANT_VM_NAME="$1"
VAGRANT_VBD_VM="$2"
VAGRANT_VPP_AGENT_ADDR="$3"
echo "---"
echo "  Running bootstrap.ubuntu1404.sh..."
echo "---"
echo "---"
echo "  VAGRANT_VM_NAME = '$VAGRANT_VM_NAME'"
echo "  VAGRANT_VBD_VM = '$VAGRANT_VBD_VM'"
echo "  VAGRANT_VPP_AGENT_ADDR = '$VAGRANT_VPP_AGENT_ADDR'"
echo "---"

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
FDIO_SNAPSHOT_URL="https://nexus.fd.io/content/repositories/fd.io.snapshot"
FDIO_DEV_URL="http://nexus.fd.io/content/repositories/fd.io.dev/"
APACHE_MAVEN_VER="apache-maven-3.3.9"
APACHE_MAVEN_TAR_GZ="$APACHE_MAVEN_VER-bin.tar.gz"
APACHE_MAVEN_URL="http://apache.go-parts.com/maven/maven-3/3.3.9/binaries/$APACHE_MAVEN_TAR_GZ"
APACHE_MAVEN_INSTALL_DIR="/usr/local/apache-maven"
GOOGLE_CHROME_DEB_PKG="google-chrome-stable_current_amd64.deb"
GOOGLE_CHROME_URL="https://dl.google.com/linux/direct/$GOOGLE_CHROME_DEB_PKG"

# Don't install VPP if this is an ODL VBD application VM
if [ "$VAGRANT_VBD_VM" != "is_vbd_vm" ] ; then
  echo "---"
  echo "  Configuring hugepages for VPP"
  echo "---"
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
  while [ "$nr_hugepages" != "1024" ] ; do
    echo "---"
    echo -n "  Allocating hugepages... "
    start hugepages
    nr_hugepages=$(cat /proc/sys/vm/nr_hugepages)
    echo "  nr_hugepages = $nr_hugepages"
    echo "---"
  done
fi

# Set prompt to include VM name if provided.
sudo -H -u vagrant perl -i -pe 's/@\\h/@\\\h\$VM_NAME/g' $VAGRANT_BASHRC
sudo -H -u vagrant touch $VAGRANT_BASH_ALIASES
if [ "$VAGRANT_VM_NAME" != "" ] && [ "$(grep VM_NAME $VAGRANT_BASH_ALIASES)" = "" ] ; then 
    echo -e "\n# Include VM Name in prompt\nexport VM_NAME=\"($VAGRANT_VM_NAME)\"" >> $VAGRANT_BASH_ALIASES
fi

# Fix grub-pc on Virtualbox with Ubuntu
export DEBIAN_FRONTEND=noninteractive

# Add fd.io apt repo in case its needed
echo "deb $FDIO_DEV_URL ./" > /etc/apt/sources.list.d/99fd.io.list

# Standard update + upgrade dance
echo "---"
echo "  Update and install ubuntu packages for development environment"
echo "---"
apt-get update
apt-get upgrade -y

# Install build tools
apt-get install -y build-essential autoconf automake bison libssl-dev ccache libtool git dkms debhelper emacs libganglia1-dev libapr1-dev libconfuse-dev git-review

# Install uio
apt-get install -y linux-image-extra-`uname -r`

# Install jdk and maven
echo "---"
echo "  Installing openjdk"
echo "---"
apt-get install -y openjdk-8-jdk
mkdir -p $APACHE_MAVEN_INSTALL_DIR
if [ -d "$KARAF_PACKAGES_MOUNT" ] ; then
  APACHE_MAVEN_TARBALL="$KARAF_PACKAGES_MOUNT/$APACHE_MAVEN_TAR_GZ"
  if [ ! -f "$APACHE_MAVEN_TARBALL" ] ; then
    echo "---"
    echo "  Downloading $APACHE_MAVEN_TAR_GZ and caching it in $KARAF_PACKAGES_MOUNT"
    echo "---"
    cd $KARAF_PACKAGES_MOUNT
    wget -q $APACHE_MAVEN_URL
    cd 
  fi
else
  echo "---"
  echo "  Downloading $APACHE_MAVEN_TAR_GZ in $APACHE_MAVEN_INSTALL_DIR"
  echo "---"
  cd $APACHE_MAVEN_INSTALL_DIR
  wget -q $APACHE_MAVEN_URL
  APACHE_MAVEN_TARBALL="$APACHE_MAVEN_INSTALL_DIR/$APACHE_MAVEN_TAR_GZ"
  cd
fi
echo "---"
echo "  Installing $APACHE_MAVEN_TARBALL in $APACHE_MAVEN_INSTALL_DIR"
echo "---"
tar -xzf $APACHE_MAVEN_TARBALL -C $APACHE_MAVEN_INSTALL_DIR
update-alternatives --install /usr/bin/mvn mvn $APACHE_MAVEN_INSTALL_DIR/$APACHE_MAVEN_VER/bin/mvn 1
update-alternatives --config mvn

# Set up Maven
sudo -H -u vagrant mkdir -p $VAGRANT_M2_DIR
sudo -H -u vagrant cp $M2_SETTINGS_XML $VAGRANT_M2_DIR
if [ "$(grep M2_HOME $VAGRANT_BASH_ALIASES)" = "" ] ; then
    cat << EOF >> $VAGRANT_BASH_ALIASES

# Maven Environment variables
export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.9
export MAVEN_OPTS="-Xms256m -Xmx512m" # Very important to put the "m" on the end
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
EOF
    chown vagrant:vagrant $VAGRANT_BASH_ALIASES
fi
source $VAGRANT_BASH_ALIASES
# Use the external Maven M2 repository as a seed if available.
if [ -d "/m2-repository" ] ; then
  sudo -H -u vagrant sed -i -e 's,/home/vagrant/.m2/repository,/m2-repository,g' /home/vagrant/.m2/settings.xml
  VAGRANT_M2_REPOSITORY="/m2-repository"
fi

# Don't install VPP and ODL Honeycomb Agent if this is an ODL VBD application VM
if [ "$VAGRANT_VBD_VM" != "is_vbd_vm" ] ; then
  # Look for karaf packages
  if [ -d "$KARAF_PACKAGES_MOUNT" ] ; then
    V3PO_TARBALL="$(find $KARAF_PACKAGES_MOUNT -name v3po-karaf*.tar.gz | sort | tail -1)"
    if [ ! -f "$V3PO_TARBALL" ] ; then
      echo "---"
      echo "  Fetching latest V3PO tarball from $FDIO_SNAPSHOT_URL"
      echo "---"
      sudo -H -u vagrant mvn dependency:get -DremoteRepositories=$FDIO_SNAPSHOT_URL -DgroupId=io.fd.honeycomb.v3po -DartifactId=v3po-karaf -Dversion=1.0.0-SNAPSHOT -Dpackaging=tar.gz -Dtransitive=false
      M2_TB_DIR="$VAGRANT_M2_REPOSITORY/io/fd/honeycomb/v3po/v3po-karaf/1.0.0-SNAPSHOT"
      V3PO_TARBALL="$(find $M2_TB_DIR -name v3po-karaf*.tar.gz | grep -v v3po-karaf-1.0.0-SNAPSHOT | sort | tail -1)"
      echo "---"
      echo "  Copying V3PO tarball ($(basename $V3PO_TARBALL)) to $KARAF_PACKAGES_MOUNT"
      echo "---"
      [ -f "$V3PO_TARBALL" ] && cp -p $V3PO_TARBALL $KARAF_PACKAGES_MOUNT
      V3PO_TARBALL="$(find $KARAF_PACKAGES_MOUNT -name v3po-karaf*.tar.gz | sort | tail -1)"
    fi
  else
    V3PO_TARBALL="$HONEYCOMB_MOUNT/v3po/karaf/target/v3po-karaf*.tar.gz"
  fi

  # Disable all ethernet interfaces other than the default route
  # interface so VPP will use those interfaces.  The VPP auto-blacklist
  # algorithm prevents the use of any physical interface contained in the
  # routing table (i.e. "route --inet --inet6") preventing the theft of
  # the management ethernet interface by VPP from the kernel.
  for intf in $(ls /sys/class/net) ; do
    if [ "$VAGRANT_VPP_AGENT_ADDR" != "" ] && [ "$(ifconfig $intf | grep $VAGRANT_VPP_AGENT_ADDR)" != "" ] ; then
      continue;
    fi
    if [ -d "/sys/class/net/$intf/device" ] && [ "$(route --inet --inet6 | grep default | grep $intf)" == "" ] ; then
        ifconfig $intf down
    fi
  done

  # Install VPP
  if [ -d "$VPP_MOUNT" ] ; then
    # Fix the silly notion that /bin/sh should point to dash by pointing it to bash
    sudo update-alternatives --install /bin/sh sh /bin/bash 100

    # Build and install VPP if necessary
    if [ -d "$VPP_BUILD_ROOT" ] ; then
      if [ "$(ls $VPP_BUILD_ROOT/*.deb)" = "" ] ; then
        echo "---"
        echo "  Building VPP"
        echo "---"
        # Bootstrap vpp
        cd $VPP_BUILD_ROOT
        sudo -H -u vagrant ./bootstrap.sh
        # Build vpp
        sudo -H -u vagrant make V=0 PLATFORM=vpp TAG=vpp_debug install-deb
      fi
      # Install debian packages
      echo "---"
      echo "  Installing VPP from $VPP_BUILD_ROOT"
      echo "---"
      dpkg -i $VPP_BUILD_ROOT/*.deb
    fi
  else
    echo "---"
    echo "  Installing VPP from nexus.fd.io"
    echo "---"
    apt-get install vpp vpp-dpdk-dev vpp-dpdk-dkms vpp-dev vpp-dbg -y --force-yes
  fi
  # Start VPP if it is installed.
  if [ "$(dpkg -l | grep vpp)" != "" ] ; then
    echo "---"
    echo "  Starting VPP"
    echo "---"
    start vpp
  fi

  # Build Honeycomb if necessary
  if [ ! -f "$V3PO_TARBALL" ] ; then
    echo "---"
    echo "  Building Honeycomb..."
    echo "---"
    cd $HONEYCOMB_MOUNT
    sudo -H -u vagrant mvn clean install -DskipTests
    V3PO_TARBALL="$(find $HONEYCOMB_MOUNT/v3po/karaf/target/ -name v3po-karaf*.tar.gz | sort | tail -1)"
  fi

  # Install honeycomb agent (v3po) if available.
  if [ -f "$V3PO_TARBALL" ] ; then
    V3PO_TARBALL_DIR="$(tar tvf $V3PO_TARBALL | head -1 | awk '{ print $6 }' | cut -d / -f 1)"
    V3PO_KARAF_DIR="$HONEYCOMB_INSTALL_DIR/$V3PO_TARBALL_DIR"
    echo "---"
    echo "  Installing Honeycomb VPP agent in $V3PO_KARAF_DIR from $V3PO_TARBALL"
    echo "---"
    [ ! -d "$HONEYCOMB_INSTALL_DIR" ] && mkdir -p $HONEYCOMB_INSTALL_DIR
    [ -d "$V3PO_KARAF_DIR" ] && rm -rf $V3PO_KARAF_DIR
    tar xzf $V3PO_TARBALL -C $HONEYCOMB_INSTALL_DIR
    $V3PO_KARAF_DIR/bin/start
    echo -e "\n\n# Add V3PO karaf bin directory to PATH\nexport PATH=\$PATH:$V3PO_KARAF_DIR/bin" >> $VAGRANT_BASH_ALIASES
  else
    echo "---"
    echo "  WARNING: V3PO Tarball is not available: $V3PO_TARBALL"
    echo "---"
  fi

# ODL VBD application VM specific installation components
else
  if [ -d "$KARAF_PACKAGES_MOUNT" ] ; then
    VBD_TARBALL="$(find $KARAF_PACKAGES_MOUNT -name vbd-karaf*.tar.gz | sort | tail -1)"
    if [ ! -f "$VBD_TARBALL" ] ; then
      echo "---"
      echo "  Fetching latest VBD tarball from $FDIO_SNAPSHOT_URL"
      echo "---"
      sudo -H -u vagrant mvn dependency:get -DremoteRepositories=$FDIO_SNAPSHOT_URL -DgroupId=io.fd.honeycomb.vbd -DartifactId=vbd-karaf -Dversion=1.0.0-SNAPSHOT -Dpackaging=tar.gz -Dtransitive=false
      M2_TB_DIR="$VAGRANT_M2_REPOSITORY/io/fd/honeycomb/vbd/vbd-karaf/1.0.0-SNAPSHOT"
      VBD_TARBALL="$(find $M2_TB_DIR -name vbd-karaf*.tar.gz | grep -v vbd-karaf-1.0.0-SNAPSHOT | sort | tail -1)"
      echo "---"
      echo "  Copying VBD tarball ($(basename $VBD_TARBALL)) to $KARAF_PACKAGES_MOUNT"
      echo "---"
      [ -f "$VBD_TARBALL" ] && cp -p $VBD_TARBALL $KARAF_PACKAGES_MOUNT
      VBD_TARBALL="$(find $KARAF_PACKAGES_MOUNT -name vbd-karaf*.tar.gz | sort | tail -1)"
    fi
  else
    VBD_TARBALL="$(find $HONEYCOMB_MOUNT/vbd/karaf/target/ -name vbd-karaf*.tar.gz | sort | tail -1)"
  fi

  # Build Honeycomb if necessary
  if [ ! -f "$VBD_TARBALL" ] ; then
    echo "---"
    echo "  Building Honeycomb..."
    echo "---"
    cd $HONEYCOMB_MOUNT
    sudo -H -u vagrant mvn clean install -DskipTests
    VBD_TARBALL="$HONEYCOMB_MOUNT/vbd/karaf/target/vbd-karaf*.tar.gz"
  fi
  # Install ODL Virtual Bridge Domain App if available.
  if [ -f "$VBD_TARBALL" ] ; then
    VBD_TARBALL_DIR="$(tar tvf $VBD_TARBALL | head -1 | awk '{ print $6 }' | cut -d / -f 1)"
    VBD_KARAF_DIR="$HONEYCOMB_INSTALL_DIR/$VBD_TARBALL_DIR"
    echo "---"
    echo "  Installing ODL Virtual Bridge Domain application in $HONEYCOMB_INSTALL_DIR/$VBD_SNAPSHOT"
    echo "---"
    [ ! -d "$HONEYCOMB_INSTALL_DIR" ] && mkdir -p $HONEYCOMB_INSTALL_DIR
    [ -d "$VBD_KARAF_DIR" ] && rm -rf $VBD_KARAF_DIR
    tar xzf $VBD_TARBALL -C $HONEYCOMB_INSTALL_DIR
    $VBD_KARAF_DIR/bin/start
    echo -e "\n\n# Add VBD karaf bin directory to PATH\nexport PATH=\$PATH:$VBD_KARAF_DIR/bin" >> $VAGRANT_BASH_ALIASES
  else
    echo "---"
    echo "  WARNING: VBD tarball is not available: $VBD_TARBALL"
    echo "---"
  fi

  # Install google chrome browser for ODL YANGUI
  echo "---"
  echo "  Installing google-chrome"
  echo "---"
  cd /opt
  wget -q $GOOGLE_CHROME_URL
  sudo dpkg -i $GOOGLE_CHROME_DEB_PKG
  sudo apt-get install -fy
fi

echo "---"
echo "  ========================="
echo "  VM Installation Complete!"
echo "  ========================="
echo "---"
