Eclipse 3.2.X instructions

---------- Required GWT variables ---------

Window->Preferences->General->Workspace->Linked Resources
Create a variable named "GWT_ROOT" pointing to your"trunk" folder.

Window->Preferences->Java->Build Path->Classpath Variables
Create a variable named "GWT_TOOLS" pointing to your"tools" folder.

---------------- Spelling -----------------

Window->Preferences->Editors->Text Editors->Spelling
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
There is no import here, make your settings match:
settings/code-style/gwt-sort-order.png

--------------- Checkstyle ----------------

1. Checkstyle is used to enforce good programming style. The Eclipse Checkstyle
plugin can be found at:

   http://eclipse-cs.sourceforge.net/
 
2. Importing the GWT Checkstyle configuration:

Window->Preferences->Checkstyle->New...
Set the Type to "External Configuration File"
Set the Name to "GWT Checks" (important)
Set the location to "settings/code-style/gwt-checkstyle.xml".
Suggested: Check "Protect Checkstyle configuration file".
Click "Ok".

3. Enabled Custom GWT Checkstyle checks:

Copy "settings/code-style/gwt-customchecks.jar" into:
 <eclipse>/plugins/com.atlassw.tools.eclipse.checkstyle_x.x.x/extension-libraries

("gwt-customchecks.jar" is also built from source into build/lib during a full build)

4. Enable Checkstyle for each project: 

Package Explorer->Project Name->Preferences->Checkstyle
Check Checkstyle active for this project, the rest of the configuration should
be already loaded.
