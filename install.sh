#!/bin/bash
#This script installs robot software dependencies on ubuntu

echo "##################################################"
echo "Installing packages required for april software"
echo "##################################################"
sudo apt-get install emacs git-core ant subversion gtk-doc-tools libglib2.0-dev libusb-1.0-0-dev gv libncurses-dev openjdk-6-jdk autopoint libgl1-mesa-dev libpng12-dev libdc1394-22-dev

echo "##################################################"
echo "Installing packages for python lcm support"
echo "##################################################"
sudo apt-get install build-essential autoconf automake autopoint python-dev python-pip

echo "##################################################"
echo "Installing Arduino Firmata Python library"
echo "##################################################"
sudo pip install pyfirmata


echo "##################################################"
echo "Installing LCM"
echo "##################################################"
cd $HOME
svn checkout http://lcm.googlecode.com/svn/trunk lcm
cd lcm
./bootstrap.sh
./configure
make
sudo make install


echo "##################################################"
echo "Setting up April class environment variables"
echo "##################################################"
export CLASSPATH=$CLASSPATH:/usr/share/java/gluegen-rt.jar:/usr/local/share/java/lcm.jar:$HOME/april/java/april.jar:./
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/april/lib
alias java='java -ea -server'
source ~/.bashrc


echo "##################################################"
echo "Installing April Toolkit"
echo "##################################################"
cd $HOME
git clone git://april.eecs.umich.edu/home/git/april.git
cd april/java
ant
