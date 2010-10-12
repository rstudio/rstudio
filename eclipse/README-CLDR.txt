Steps to process CLDR data using Eclipse:

1) Get the CLDR data available locally on your system, such as by:
  svn co http://unicode.org/repos/cldr/tags/release-1-8-1 <cldrdir>
2) Set a linked resource variable pointing to this in Eclipse:
    Window -> Preferences -> General -> Workspace -> Linked Resources
  Add a variable CLDR_ROOT pointing to <cldrdir> above
3) Import the cldr-data, cldr-tools, and cldr-import projects (note that
  CLDR_ROOT must be defined as above, or you will have to delete and
  reimport these projects).
    File -> Import -> General -> Existing Projects in Workspace
  Browse to $GWT_ROOT/eclipse and select the cldr-data, cldr-tools,
  and cldr-import projects.
4) Run the GenerateGwtCldrData launch config -- by default, it will
  overwrite files in the GWT distribution; edit the --outdir argument
  if you want it to go somewhere else.
