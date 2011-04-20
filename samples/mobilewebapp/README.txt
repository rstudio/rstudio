-- Preparation --

Use Ant to build this project (http://ant.apache.org/). Ant uses the
'build.xml' file in this folder, which describes exactly how to build
your project.  This file has been tested to work against Ant 1.7.1.
The following assumes 'ant' is on your command line path.

This project uses Google App Engine
(https://appengine.google.com/). You can download the Google App
Engine SDK from http://code.google.com/appengine/. The Ant build.xml
script needs to know where your Google App Engine SDK is
installed. Create a file named local.properties in the same folder as
the build.xml file. It needs to contain a definition for the
appengine.sdk property. For example, if the Google App Engine SDK is
installed at /opt/appengine-sdk, a line in the local.properties file
should contain:

appengine.sdk=/opt/appengine-sdk

-- Build from the command line with Ant --

To run development mode, just type 'ant devmode'.

To compile your project for deployment, just type 'ant'.

To compile and also bundle into a .war file, type 'ant war'.

For a full listing of other targets, type 'ant -p'.
