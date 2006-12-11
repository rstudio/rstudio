Eclipse 3.2.X instructions

Code style/formatting:
Window->Preferences->Java->Code Style->Formatter->Import...
gwt-format.xml

Import organization:
Window->Preferences->Java->Code Style->Organize Imports->Import...
gwt.importorder

Member sort order:
Window->Preferences->Java->Appearance->Members Sort Order
There is no import here, make your settings match gwt-sort-order.png

Checkstyle:
1. Checkstyle is used to enforce good programming style. The Eclipse Checkstyle
plugin can be found at:                      http://eclipse-cs.sourceforge.net/.
 
2. Importing the GWT Checkstyle configuration:
Window->Preferences->Checkstyle->New...
Set the Type to "External Configuration File"
Set the Name to "GWT Checks"
Set the location to the "gwt-checkstyle.xml" in this directory.
Click "Ok".

3. Enabled Custom GWT Checkstyle checks:
After building GWT, a "gwt-customchecks.jar" is created in build/lib.  
Copy this jar into:
 <eclipse>/plugins/com.atlassw.tools.eclipse.checkstyle_x.x.x/extension-libraries


4. Enable Checkstyle for each project: 
Package Explorer-> Project Name -> Preferences->Checkstyle
Check Checkstyle active for this project, the rest of the configuration should
be already loaded.
