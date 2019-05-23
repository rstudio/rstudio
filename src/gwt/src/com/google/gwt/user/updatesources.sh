#!/bin/sh

# Overwrites customized files with originals from gwt-user.jar. This should
# be done with each new release of GWT to make sure we're not pounding over
# changes made by the GWT team.

cd ../../../../
pwd

# extract over previously modified sources
jar xvf ../lib/gwt/2.8.2/gwt-user.jar \
  com/google/gwt/user/client/ui/MenuBar.java \
  com/google/gwt/user/client/ui/SplitPanel.java \
  com/google/gwt/user/client/ui/SplitLayoutPanel.java

cd com/google/gwt/user/client/ui

# apply previous patches
patch MenuBar.java < MenuBar.java.diff
patch SplitPanel.java < SplitPanel.java.diff
patch SplitLayoutPanel.java < SplitLayoutPanel.java.diff

