You will need to checkout the SDKs required for building the plugin
separately.  These are located at:
	https://google-web-toolkit.googlecode.com/svn/plugin-sdks

This assumes the SDKS are located in ../../../plugin-sdks -- if this is
not correct, edit the definition in Makefile or pass PLUGIN_SDKS=<path> on the
make command.

Build by:

make ARCH=x86 BROWSER=ff35
make ARCH=x86_64 BROWSER=ff3

etc -- default is current architecture and ff3.

BROWSER values supported:
  ff3	Firefox 3.0
  ff3+  Firefox 3.0.11+ on some platforms
  ff35  Firefox 3.5
  ff36  Firefox 3.6
  ff40  Firefox 4.0 (and 3.7alpha)

You may need to try both ff3 and ff3+, as different platforms chose different
library layouts.

Targets of interest:
  make linuxplatforms
  make macplatforms
