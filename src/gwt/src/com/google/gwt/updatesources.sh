#!/bin/sh

# Overwrites customized files with originals from gwt-user.jar, then reapplies
# previous patches.

GWTVER=2.8.2
cd ../../../
SRCBASE=`pwd`

# extract over previously modified sources
jar xvf ${SRCBASE}/../lib/gwt/${GWTVER}/gwt-user.jar \
  com/google/gwt/user/client/ui/MenuItem.java \
  com/google/gwt/user/client/ui/DecoratorPanel.java \
  com/google/gwt/user/client/ui/VerticalPanel.java \
  com/google/gwt/user/client/ui/HorizontalPanel.java \
  com/google/gwt/user/client/ui/MenuBar.java \
  com/google/gwt/user/client/ui/SplitPanel.java \
  com/google/gwt/user/client/ui/SplitLayoutPanel.java
  
jar xvf ${SRCBASE}/../lib/gwt/${GWTVER}/gwt-dev.jar \
  com/google/gwt/core/linker/CrossSiteIframeLinker.java \
  com/google/gwt/core/ext/linker/impl/installLocationIframe.js

# apply previous patches
cd ${SRCBASE}/com/google/gwt/user/client/ui
patch MenuItem.java < MenuItem.java.diff
patch DecoratorPanel.java < DecoratorPanel.java.diff
patch VerticalPanel.java < VerticalPanel.java.diff
patch HorizontalPanel.java < HorizontalPanel.java.diff
patch MenuBar.java < MenuBar.java.diff
patch SplitPanel.java < SplitPanel.java.diff
patch SplitLayoutPanel.java < SplitLayoutPanel.java.diff

cd ${SRCBASE}/com/google/gwt/core/ext/linker/impl
patch XinstallLocationIframe.js < installLocationIframe.js.diff

cd ${SRCBASE}/com/google/gwt/core/linker
patch XCrossSiteIframeLinker.java < CrossSiteIframeLinker.java.diff
