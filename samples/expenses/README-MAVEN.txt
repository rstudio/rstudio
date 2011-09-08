-- Option A: Import your project into Eclipse (recommended) --

Configure Eclipse following the instructions at 
http://code.google.com/p/google-web-toolkit/wiki/WorkingWithMaven#Using_Maven_with_Google_Plugin_for_Eclipse

In Eclipse, go to the File menu and choose:

  File -> Import... -> Existing Maven Projects into Workspace

  Browse to the directory containing this file,
  select "Expenses".

  Click Finish.

You can now browse the project in Eclipse.

To launch your web app in GWT development mode

  Go to the Run menu item and select Run -> Run as -> Web Application.

  - To load a set of initial data choose: LoadExpensesDB.html

  - To run the Expenses Application choose: Expenses.html

  - To run the Mobile version of the Expenses Application choose:
    ExpensesMobile.html

  When prompted for which directory to run from, simply select the directory
  that Eclipse defaults to.

  You can now use the built-in debugger to debug your web app in development mode.

GWT developers (those who build GWT from source) may add their
gwt-user project to top of this project's class path in order to use
the built-from-source version of GWT instead of the version specified
in the POM.

  Select the project in the Project explorer and select File > Properties

  Select Java Build Path and click the Projects tab

  Click Add..., select gwt-user, and click OK

  Click Order and Export and move gwt-user above Maven Dependencies

-- Option B: Build from the command line with Maven --

If you prefer to work from the command line, you can use Maven to
build your project (http://maven.apache.org/). You will also need Java
1.6 JDK. Maven uses the supplied 'pom.xml' file which describes
exactly how to build your project. This file has been tested to work
against Maven 2.2.1. The following assumes 'mvn' is on your command
line path.

To run development mode use the Maven GWT Plugin.

  cd src/main/webapp; mvn -f ../../../pom.xml gwt:run

To compile your project for deployment, just type 'mvn package'.

For a full listing of other goals, visit:
http://mojo.codehaus.org/gwt-maven-plugin/plugin-info.html
