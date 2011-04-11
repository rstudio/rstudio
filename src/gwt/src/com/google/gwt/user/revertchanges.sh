#!/bin/sh

# Overwrites customized files with originals from gwt-user.jar. This should
# be done with each new release of GWT to make sure we're not pounding over
# changes made by the GWT team.

cd ../../../../
pwd
jar xvf ../lib/gwt/2.3.0-m1/gwt-user.jar \
  com/google/gwt/user/client/ui/MenuBar.java \
  com/google/gwt/user/client/ui/SplitPanel.java \
  com/google/gwt/user/client/ui/SplitLayoutPanel.java
