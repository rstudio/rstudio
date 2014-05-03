Steps to process CLDR data using Eclipse:

1) Get latest CLDR data available locally on your system and compile
   it, such as by:
  $ svn co http://unicode.org/repos/cldr/tags/release-25 <cldrdir>
  $ cd release-25/tools/java
  $ ant clean jar
2) Required CLDR variables in Eclipse
  Set a linked resource variable pointing to this:
    Window -> Preferences -> General -> Workspace -> Linked Resources
    Add a variable CLDR_ROOT pointing to <cldrdir> above
  Create a classpath variable pointing to the same folder:
    Window -> Preferences -> Java -> Build Path -> Classpath Variables
    Create a variable named CLDR_ROOT pointing to <cldrdir>
  Create a string subtitution variable:
    Window -> Preferences -> Run/Debug -> String Substitution -> New
    Create the variable CLDR_ROOT pointing to <cldrdir>
3) Import the cldr-data, cldr-tools, and cldr-import projects (note that
  CLDR_ROOT must be defined as above, or you will have to delete and
  reimport these projects).
    File -> Import -> General -> Existing Projects in Workspace
  Browse to $GWT_ROOT/eclipse and select the cldr-data, cldr-tools,
  and cldr-import projects.
4) Set Compiler compliance level to 1.7
    Project -> Properties -> Java Compiler -> Enable project specific settings
    -> Compiler compliance level 1.7 -> use default compliance settings.
5) Run the GenerateGwtCldrData launch config -- by default, it will
  overwrite files in the GWT distribution; edit the --outdir argument
  if you want it to go somewhere else.
