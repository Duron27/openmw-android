#!/bin/bash

# Update package list and install packages
pkg install -y rust cmake make python3 git ccache p7zip openssl perl;

# Create variables
export TERMUX="/data/data/com.termux/files";
alias tes3cmd='perl tes3cmd';
alias umo='python ~/umo/src/umo.py';

# Clone the repository
git clone https://gitlab.com/modding-openmw/umo.git;
git clone https://gitlab.com/modding-openmw/tes3cmd.git;

# Install pip stuff
pip install -r umo/requirements.txt

# Add to bin
cp $TERMUX/home/tes3cmd/tes3cmd $TERMUX/usr/bin;

# Create folders and config file
mkdir -p ~/.config/umomwd
echo '{"NEXUS_API_KEY": "", "TES3CMD": "/data/data/com.termux/files/usr/bin/tes3cmd", "MORROWIND_DIR": "/storage/emulated/0/", "BASEPATH": "/storage/emulated/0/Download/OpenMW/mods", "CACHE_DIR": "/storage/emulated/0/Download/OpenMW/CACHE"}' > ~/.config/umomwd/config.json

echo 'export PATH=${PATH}:~/umo/src:~/tes3cmd' > ~/.bashrc;
echo 'alias tes3cmd='perl tes3cmd'' > ~/.bashrc;
echo 'alias umo='python ~/umo/src/umo.py'' > ~/.bashrc;
source ~/.bashrc;

# Print success message
echo "Ready to install mods!";

