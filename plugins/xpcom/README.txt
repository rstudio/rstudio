You will need to checkout the SDKs required for building the plugin
separately.  These are located at:
	https://google-web-toolkit.googlecode.com/svn/plugin-sdks

This assumes the SDKS are located in ../../../plugin-sdks -- if this is
not correct, edit the definition in Makefile.

Build by:

make ARCH=x86 BROWSER=ff2
make ARCH=x86_64 BROWSER=ff3

etc -- default is current architecture and ff3.

BROWSER values supported:
  ff2	Firefox 1.5-2.0
  ff3	Firefox 3.0
  ff3+  Firefox 3.0.11+ on some platforms
  ff35  Firefox 3.5

You may need to try both ff3 and ff3+, as different platforms chose different
library layouts.

In the future, we will try and make a combined XPI which uses a JS shim plugin
to select the proper shared library file to use based on the platform.
