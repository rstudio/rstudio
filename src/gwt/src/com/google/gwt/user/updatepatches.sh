#!/bin/sh

# Extract originals, create patches, delete original.

cd ../../../
mkdir tmp
cd tmp

jar xvf ../../../lib/gwt/2.8.2/gwt-user.jar \
  com/google/gwt/user/client/ui/VerticalPanel.java \
  com/google/gwt/user/client/ui/HorizontalPanel.java \
  com/google/gwt/user/client/ui/MenuBar.java \
  com/google/gwt/user/client/ui/SplitPanel.java \
  com/google/gwt/user/client/ui/SplitLayoutPanel.java

cd ../google/gwt/user/client/ui

diff ../../../../../tmp/com/google/gwt/user/client/ui/VerticalPanel.java VerticalPanel.java > VerticalPanel.java.diff
diff ../../../../../tmp/com/google/gwt/user/client/ui/HorizontalPanel.java HorizontalPanel.java > HorizontalPanel.java.diff
diff ../../../../../tmp/com/google/gwt/user/client/ui/MenuBar.java MenuBar.java > MenuBar.java.diff
diff ../../../../../tmp/com/google/gwt/user/client/ui/SplitPanel.java SplitPanel.java > SplitPanel.java.diff
diff ../../../../../tmp/com/google/gwt/user/client/ui/SplitLayoutPanel.java SplitLayoutPanel.java > SplitLayoutPanel.java.diff

rm -rf ../../../../../tmp

