This is a quick and dirty Wireshark packet dissector for the GWT Code Server
protocol.

I have only tested this on Ubuntu Hardy with wireshark 1.0.0 on an x86_64
machine.  It may require other changes for other platforms, and has only
light testing.  It is also incomplete but provided enough decoding to be
useful to me -- YMMV.

The Makefile is very Unix-centric as it installs the library under your home
directory.

On Linux, you need the wireshark-dev package installed.
