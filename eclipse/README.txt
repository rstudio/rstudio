Eclipse 3.2.X instructions

---------- Required GWT variables ---------

Window->Preferences->General->Workspace->Linked Resources
Create a variable named "GWT_ROOT" pointing to your "trunk" folder.

Window->Preferences->Java->Build Path->Classpath Variables
Create a variable named "GWT_TOOLS" pointing to your "tools" folder.
Create a variable named "JDK_HOME" pointing to the root of your JDK install
  (for example, C:\Program Files\jdk1.5.0_05 or /usr/lib/j2sdk1.5-sun)

---------------- Spelling -----------------

Window->Preferences->General->Editors->Text Editors->Spelling
Enable spell checking, use "settings/english.dictionary".

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
3) Static Initialzers
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

--------------- Checkstyle ----------------
Checkstyle is used to enforce good programming style.

1. Install Checkstyle

The Eclipse Checkstyle plugin can be found at:

   http://eclipse-cs.sourceforge.net/
 
2. Enable Custom GWT Checkstyle checks:

Copy "settings/code-style/gwt-customchecks.jar" into:
 <eclipse>/plugins/com.atlassw.tools.eclipse.checkstyle_x.x.x/extension-libraries

Restart Eclipse.
("gwt-customchecks.jar" is also built from source into build/lib during a full build)

3. Import GWT Checks:

Window->Preferences->Checkstyle->New...
Set the Type to "External Configuration File"
Set the Name to "GWT Checks" (important)
Set the location to "settings/code-style/gwt-checkstyle.xml".
Suggested: Check "Protect Checkstyle configuration file".
Click "Ok".

4. Import GWT Checks for Tests

Repeat step 2, except:
Set the Name to "GWT Checks for Tests" (important)
Set the location to "settings/code-style/gwt-checkstyle-tests.xml".
