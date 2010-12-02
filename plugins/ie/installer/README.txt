This directory contains the source code for the Windows installer. A new 
installer should be built using the build script every time oophm.dll is 
replaced.

The build script will only work on a Windows system


Workflow:
~~~~~~~~~
if you build new binaries, do the following:

1) check out ..\prebuilt\*.msi for editing
  
2) run build <version>, as in: build 1.2.123
   this will generated all the msis. make sure to look at ..\prebuilt\*.msi 
   to see if they were updated.

4) Test the installation. Yes, make sure it works on x86, x64, it cleans the 
   registry and folder upon uninstall, etc.
   
	
