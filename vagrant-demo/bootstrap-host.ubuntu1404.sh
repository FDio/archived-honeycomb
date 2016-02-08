# Args
VAGRANT_VM_NAME="$1"
echo "Running bootstrap-host.ubuntu1404.sh..."
echo "VAGRANT_VM_NAME = '$VAGRANT_VM_NAME'"

# Directory and file definitions
VAGRANT_HOME="/home/vagrant"
VAGRANT_BASHRC="$VAGRANT_HOME/.bashrc"
VAGRANT_BASH_ALIASES="$VAGRANT_HOME/.bash_aliases"

# Set prompt to include VM name if provided.
sudo -H -u vagrant perl -i -pe 's/@\\h/@\\\h\$VM_NAME/g' $VAGRANT_BASHRC
sudo -H -u vagrant touch $VAGRANT_BASH_ALIASES
if [ "$VAGRANT_VM_NAME" != "" ] && [ "$(grep -q VM_NAME $VAGRANT_BASH_ALIASES)" = "" ] ; then 
    echo -e "\n# Include VM Name in prompt" >> $VAGRANT_BASH_ALIASES
    echo "export VM_NAME=\"($VAGRANT_VM_NAME)\"" >> $VAGRANT_BASH_ALIASES
fi
chown vagrant:vagrant $VAGRANT_BASH_ALIASES

# Fix grub-pc on Virtualbox with Ubuntu
export DEBIAN_FRONTEND=noninteractive

# Add fd.io apt repo in case its needed
echo "deb http://nexus.fd.io/content/repositories/fd.io.dev/ ./" > /etc/apt/sources.list.d/99fd.io.list

# Standard update + upgrade dance
apt-get update
apt-get upgrade -y
apt-get install -y wireshark
