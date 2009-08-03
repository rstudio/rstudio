This directory contains the source for the OOPHM plugin which resides in
the browser to allow hosted mode debugging.

Subdirectories:
 - common
   Code that is shared between all platforms, and mostly deals with the wire
   protocol and related tables.

 - ie
   Plugin for 32-bit Internet Explorer

 - npapi
   Obsolete NPAPI plugin for Firefox

 - webkit
   plugin for WebKit-based browsers that support its extension mechanism
   (ie, no Safari/Windows support)

 - xpcom
   XPCOM-based Firefox plugin

To build, see instructions in each directory.
