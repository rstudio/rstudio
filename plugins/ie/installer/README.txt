This directory contains the source code for the Windows installer. A new installer should
be built using the build script every time oophm.dll is replaced.

The build script will only work on a Windows system with WIX installed:
http://wix.sourceforge.net/


Files contained in this directory:
installer.wxs.xml - the WIX script used to generate the installer

GwtDevModeIePluginInstaller.msi - the windows installer, which is replaced when the build script is executed

GwtDialog.bmp - the image displayed on the first page of the installer

GwtBanner.bmp - the image displayed at the top of every page other than the first in the installer

COPYING.rtf - Our license agreement in RTF format

build.xml - the ant build script