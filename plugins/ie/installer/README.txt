This directory contains the source code for the Windows installer. A new installer should
be built using the build script every time oophm.dll is replaced.

The build script will only work on a Windows system with WIX installed:
http://wix.sourceforge.net/


Files contained in this directory:
installer.wxs.xml - the WIX script used to generate the installer

build.xml - the ant build script