Eclipse 3.X instructions

These instructions are intended for contributors to the GWT source
code repository that want to run the Eclipse IDE. It describes how to
configure Eclipse for the correct coding styles and how to setup a GWT
project for debugging the core GWT code.


== Configure Eclipse Environment==

All relative paths are relative to the GWT source repository's
'trunk/eclipse' folder.

---------- Required GWT variables ---------

Window->Preferences->General->Workspace->Linked Resources
Create a variable named "GWT_ROOT" pointing to your "trunk" folder.

Window->Preferences->Java->Build Path->Classpath Variables
Create a variable named "GWT_TOOLS" pointing to your "tools" folder.
Create a variable named "JDK_HOME" pointing to the root of your JDK install
  (for example, C:\Program Files\jdk1.6 or /usr/lib/j2sdk1.6-sun)

---------------- Encoding -----------------

Window->Preferences->General->Workspace->Text file encoding
Use "UTF-8"

---------------- Spelling -----------------

Window->Preferences->General->Editors->Text Editors->Spelling
Enable spell checking
Use "settings/english.dictionary".

------------ Output Filtering -------------

Window->Preferences->Java->Compiler->Building
Make sure "Filtered Resources" includes ".svn/"

---------- Code style/formatting ----------

Window->Preferences->Java->Code Style->Formatter->Import...
  settings/code-style/gwt-format.xml

----------- Import organization -----------

Window->Preferences->Java->Code Style->Organize Imports->Import...
  settings/code-style/gwt.importorder

------------ Member sort order ------------

Window->Preferences->Java->Appearance->Members Sort Order
There is no import here, so make your settings match:
  settings/code-style/gwt-sort-order.png

First, members should be sorted by category.
1) Types
2) Static Fields
3) Static Initializers
4) Static Methods
5) Fields
6) Initializers
7) Constructors
8) Methods

Second, members in the same category should be sorted by visibility.
1) Public
2) Protected
3) Default
4) Private

Third, within a category/visibility combination, members should be sorted
alphabetically.
 

------------ Compiler settings ------------
Window->Preferences->Java->Compiler
Set the compiler compliance level to 1.6.

------- Compiler errors & warnings --------
Window->Preferences->Java->Compiler->Errors/Warnings

The following warnings are suggested.

Code Style:
- Method with a constructor name

Potential programming problems:
- Assignment has no effect
- Accidental boolean assignment
- 'finally' does not complete normally
- Using a char array in string concatentation
- Hidden catch block
- Inexact type match for vararg arguments

Name shadowing and conflicts: all except "Local variable" hiding

Deprecated and restricted API: all

Unnecessary code: all except "Unnecessary 'else' statement"

Generic types: all except "Generic type parameter declared with final type bound"

Annotations:
- Annotation is used as super interface
- Enable @SuppressWarnings annotations

------- Annotation Processor --------

Some components of GWT (RequestFactory) use annotation processing. Do
the following to add a directory named '.apt-generated' to the source
path, and put on the APT factory path annotation processors required
by the RequestFactory unit tests.

Select project 'gwt-user'

Project->Properties->Java Compiler->AnnotationProcessor
- Check Enable project specific settings
- Check Enable annotation processing
- Optional: check Enable processing in editor (for on-the-fly
  validation of RequestFactory interfaces)

Project->Properties->Java Compiler->AnnotationProcessor->Factory Path
- Add Variable
- Select GWT_TOOLS and click Extend
- Select lib/requestfactory and the most recently dated
  requestfactory-apt-YYYY-MM-DD.jar
- Optional: deselect org.eclipse.jst.ws.annoations.core if eclipse has
  added it, it can make eclipse sluggish

== Checkstyle ==

Checkstyle is used to enforce good programming style. Its use in
Eclipse is optional, since it is also run as part of the acceptance
test suite.

1. Install Checkstyle version 5.7.

The Eclipse Checkstyle plugin can be found at:
  http://eclipse-cs.sourceforge.net/

2. Import GWT Checks:

Window->Preferences->Checkstyle->New...
Set the Type to "External Configuration File"
Set the Name to "GWT Checks" (important)
Set the location to "settings/code-style/gwt-checkstyle.xml".
Suggested: Check "Protect Checkstyle configuration file".
Click "Ok".

3. Import GWT Checks for Tests

Repeat step 2, except:
Set the Name to "GWT Checks for Tests" (important)
Set the location to "settings/code-style/gwt-checkstyle-tests.xml".

------------ GWT dev-jar path ------------
To run applications against the Eclipse projects for GWT rather than an
extracted jar file, you need to tell GWT where to find its native libraries
since they won't be in the jar file.  Using this approach requires building
GWT from the command line with ant once to get the files in the proper
location, but it doesn't need to be redone unless you change any of the native
libraries.

Window->Preferences->Run/Debug->String Substitution->New...
Set the Name to "gwt_devjar".
Set the Value to the appropriate path for your install -- for example:
  <path-to-trunk>\trunk\build\staging\gwt-0.0.0\gwt-dev.jar
  <path-to-trunk>/trunk/build/staging/gwt-0.0.0/gwt-dev.jar
Description can be left blank.


== Importing the GWT core projects ==

1) Import the 'gwt-dev' and 'gwt-user' projects

  File->Import->General->Existing Projects into Workspace->Next
  Browse to the 'trunk/eclipse' folder and select it
  Deselect All

  Inside this folder are a number of .projects files, only a few of
  which you will need to get started. You may import others later.

  Select 'gwt-dev'
  Select 'gwt-user'
  Select any of the GWT samples as you want.  The most useful ones are:
    - Hello: very simple project useful as a little playground
    - Showcase: complex UI application
    - DynaTable: uses RPC
  Then press the Finish button.

2) If you are using the Google Plugin for Eclipse 
  (http://code.google.com/eclipse), enable it for 'gwt-user'

  Right click the 'gwt-user' project and go to properties. Select 
  Google->Web Toolkit, and check the box for 'Use Google Web Toolkit'.

  Then, select Google->Web Application, and uncheck 'This project has a WAR
  directory'. Then press the "Apply" button.

3) Dismiss the welcome tab if you are setting up an Eclipse workspace
  for the first time.

  You should now have several new projects in your Eclipse workspace.
  If you are lucky, they will compile too!

  If they did not compile, recheck the setting of

     - GWT_ROOT
     - GWT_TOOLS
     - JDK_HOME

  Then refresh each project.

4) Finally, drop to the command line and build the project
  using 'ant'. You may need to first download ant from the web:

    http://ant.apache.org/

  Run the ant installation procedure.

  Before you continue, make sure that the 'ant' binary is on your path.

    $ cd <gwt>/trunk/
    $ ant

  This only has to be done once to create the 'trunk/build/staging/...'
  directories.  After you build from the command line  once, you can
  use Eclipse's built in compiler.


== Launching 'Hello' ==

While the 'applicationCreator' script is useful for setting up projects and
launch configurations that target a GWT installation, it is not intended for
GWT developers working against the source code.  You will want to run not
against .jar files, but against the class files built by Eclipse.  The
following instructions help you do just that.

1) Import the 'Hello' project if you haven't already.

  File->Import->General->Existing Projects into Workspace->Next
  Browse to the 'trunk/eclipse' folder and select it
  Deselect All
  Select 'Hello'

2) Now you should be able to run the 'Hello' project from the
  Run dialog!


== Creating a Launch config for a new project ==

The simplest way to create a new launch config is to use the Run dialog to
duplicate the 'Hello.launch' and 'Hello compile.launch' configurations and
then edit the arguments and classpath settings to match your new project.


== Recreating a Launch configuration from scratch ==

This section captures the process used to create the original 'Hello.launch'

1) Create or Import a new project

  Using the 'applicationCreator' script is an easy way to do this, but you
  cannot use the created launch scripts to develop the GWT core source because
  they are configured to run with .jar files from a GWT installation.

2) Add a project reference to the gwt-user project:

 Project->Preferences...->Projects Tab->Add...
 Add 'gwt-user' as a project dependency.

3) Create a new launch configuration

  Select the project in the tree on the left of the Main Window
  Open the Run... dialog

  Create a new Java Application.

  Main Tab:
  Name: Debug Launch Config
  Project: <your project>
  Select the checkbox "Include inherited mains when searching for a main class"
  Main class: com.google.gwt.dev.GWTShell
  (Note: the 'Search' button may not work, just type it in without a search.)

Arguments Tab:
   In the 'Program arguments' text area, add the name of the module
   host web page and any other hosted mode arguments:

    -out www
    <your module package>.<module name>/<module name>.html

  e.g.
    -out
    www com.google.gwt.sample.hello.Hello/Hello.html

  In the 'VM arguments' text area, add the following:

  -Dgwt.devjar=${gwt_devjar}

  This is a very obscure way of telling GWT where to find the C++ libraries
  located in that directory, and the gwt_devjar variable should be set as
  described above.  If you do not have this set, or if you have not build GWT
  at least once using ant, you will see an exception like this at startup:

   Exception in thread "main" java.lang.ExceptionInInitializerError
   Caused by: java.lang.RuntimeException: Installation problem detected,
   please reinstall GWT


  Other VM arguments you might want to add:
    -ea  (enable assertions)
    -server (enable java server VM)
    -Xcheck:jni (adds extra checks before passing args to a JNI method)

Classpath:
  Click on 'User Entries' and use the 'Advanced' button to add the following folders:
  <project>/src
  gwt-user/core/src
  gwt-user/core/super
  gwt-dev/core/super

  Now, select the default classpath (Hello) and move it all the way
  to the bottom of the list using the 'Down' button.

  You should now be able to run the application
  using the Eclipse launcher.
