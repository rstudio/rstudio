Eclipse 3.2.X instructions

Code style/formatting:
Window->Preferences->Java->Code Style->Formatter->Import...->gwt-format.xml

Import organization:
Window->Preferences->Java->Code Style->Organize Imports->Import...->gwt.importorder

Member sort order:
Window->Preferences->Java->Appearance->Members Sort Order
There is no import here, make your settings match gwt-sort-order.png

Checkstyle:
1. Checkstyle is used to enforce good programming style. The Eclipse Checkstyle plugin can be found at:                                                                                                                                                                                                                                                                                       
http://eclipse-cs.sourceforge.net/.
 
2. Importing the GWT Checkstyle configuration:
Window->Preferences->Checkstyle->new->import->gwtCheckstyle.xml as GWT Checks


3. Enabled Custom GWT Checkstyle checks:
After the first ant-build, CustomChecks.jar is created in build/lib.  
Go to <path-to-my-eclipse>/plugins/com.atlassw.tools.eclipse.checkstyle_x.x.x directory and copy the CustomChecks.jar into the extension-libraries directory


4. Enable Checkstyle for each project: 
Package Explorer-> Project Name -> Preferences->Checkstyle
       Check Checkstyle active for this project, the rest of the configuration should be already loaded.
 	



