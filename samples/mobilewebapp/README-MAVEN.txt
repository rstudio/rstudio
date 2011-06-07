-- Option A: Import your project into Eclipse (recommended) --

If you use Eclipse for Jave EE, you can simply import the generated
project into Eclipse. We've tested against Eclipse 3.6. Later versions
will likely also work, earlier versions may not. (Please note that
this demo requires WTP, which is pre-installed with the Jave EE
versions of Eclipse.)

Eclipse users will need to install the following plugin components:
- Google Plugin for Eclipse (instructions at http://code.google.com/eclipse/)
- m2eclipse Core
- Maven Integration for WTP (in m2eclipse extras)
  Instructions for installing the maven plugins can be found here:
  http://m2eclipse.sonatype.org/installing-m2eclipse.html

Ensure Eclipse is configured to use Java 1.6 as this sample uses
AppEngine.

In Eclipse, go to the File menu and choose:

  File -> Import... -> Existing Maven Projects into Workspace

  Select the directory containing this file.

  Click Finish.

You can now browse the project in Eclipse.

To launch your web app in GWT development mode

  Go to the Run menu item and select Run -> Run as -> Web Application.

  If prompted for which directory to run from, simply select the directory
  that Eclipse defaults to.

  You can now use the built-in debugger to debug your web app in development mode.

GWT developers (those who build GWT from source) may add their
gwt-user and gwt-dev projects to this project's class path in order to
use the built-from-source version of GWT instead of the version
specified in the POM.

  Select the project in the Project explorer and select File > Properties

  Select Java Build Path and click the Projects tab

  Click Add..., select gwt-user and gwt-dev, and click OK

  Still in the Java Build Path dialog, click the Order and Export tab

  Move gwt-dev and gwt-user above Maven Dependencies
  
GWT developers can also use tools/scripts/maven_script.sh to push their
own GWT jars into their local maven repo.

-- Option B: Build from the command line with Maven --

If you prefer to work from the command line, you can use Maven to
build your project (http://maven.apache.org/). You will also need Java
1.6 JDK. Maven uses the supplied 'pom.xml' file which describes
exactly how to build your project. This file has been tested to work
against Maven 2.2.1. The following assumes 'mvn' is on your command
line path.

To run development mode use the Maven GWT Plugin.

  mvn gwt:run

To compile your project for deployment, just type 'mvn package'.

For a full listing of other goals, visit:
http://mojo.codehaus.org/gwt-maven-plugin/plugin-info.html
