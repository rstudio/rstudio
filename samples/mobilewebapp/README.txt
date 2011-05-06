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

-- Building with Eclipse and the Google Plugin for Eclipse --

If you use Eclipse, you can simply import the generated project into
Eclipse.  We've tested against Eclipse 3.5 and 3.6 and GPE 2.3.  Later
versions will likely also work, earlier versions may not.

1. In Eclipse, make sure the App Engine SDK is configured in

  Window -> Properties -> Google -> App Engine

2. Go to the File menu and choose:

  File -> Import... -> Existing Projects into Workspace

  Browse to the directory containing this file,
  select "MobileWebApp".

  Be sure to uncheck "Copy projects into workspace" if it is checked.
  
  Click Finish.

  You can now browse the project in Eclipse.

3. Eclipse may produce warings indicating that

  "The App Engine SDJ JAR xx.yy.jar is missing in the WEB-INF/lib directory"

  Select the warning, right click and choose

  "Quick fix" -> "Synchronize <WAR>/WEB-INF/lib with SDK libraries"

  Click "Finish"

4. Eclipse may also produce a warning indicating that

  "The GWT SDK JAR gwt-servlet.jar is missing in the WEB-INF/lib directory"

  Select the warning, right click and choose

  "Quick fix" -> "Synchronize <WAR>/WEB-INF/lib with SDK libraries"

  Click "Finish"

5. To launch your web app in GWT development mode, go to the Run menu and choose:

  Run -> Open Debug Dialog...

  Under Java Application, you should find a launch configuration
  named "MobileWebApp".  Select and click "Debug".

  You can now use the built-in debugger to debug your web app in development mode.
