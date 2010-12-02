GWT devmode plugin for IE
~~~~~~~~~~~~~~~~~~~~~~~~~

To build the plugin, you will need a windows system with Visual Studio 2008 or newer (WSDK 7.0+)

To build:
1) make sure devenv.exe is in the path
2) make sure the binaries under $(projectroot)\third_party\java_src\gwt\svn\trunk\plugins\ie\prebuilt are writable.
3) open a cmd.exe window, go to $(projectroot)\third_party\java_src\gwt\svn\trunk\plugins\ie
4) run build.cmd

To create msi installer:
1) go to folder $(projectroot)\third_party\java_src\gwt\svn\trunk\plugins\ie\installer
2) make sure the msi installers under $(projectroot)\third_party\java_src\gwt\svn\trunk\plugins\ie\prebuilt are writable.
3) run build.cmd


