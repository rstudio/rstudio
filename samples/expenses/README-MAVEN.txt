-- Option A: Import your project into Eclipse (recommended) --

If you use Eclipse, you can simply import the generated project into
Eclipse. We've tested against Eclipse 3.5. Later versions will likely
also work, earlier versions may not.

Eclipse users will need to have the m2eclipse, or equivalent, pluigin
installed. Instructions for how to install the m2eclipse plugin can
be found here: http://m2eclipse.sonatype.org/installing-m2eclipse.html

Ensure Eclipse is configured to use Java 1.6 as this sample uses
AppEngine.

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
