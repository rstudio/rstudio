#!/bin/sh

# Overwrites customized files with originals from gwt-user.jar, then reapplies
# previous patches.

GWTVER=2.8.2
cd ../../../
SRCBASE=`pwd`

# extract over previously modified sources
jar xvf ${SRCBASE}/../lib/gwt/${GWTVER}/gwt-user.jar \
  com/google/gwt/user/cellview/client/AbstractHeaderOrFooterBuilder.java \
  com/google/gwt/user/client/ui/CellPanel.java \
  com/google/gwt/user/client/ui/DecoratorPanel.java \
  com/google/gwt/user/client/ui/FormPanel.java \
  com/google/gwt/user/client/ui/Label.java \
  com/google/gwt/user/client/ui/LabelBase.java \
  com/google/gwt/user/client/ui/MenuBar.java \
  com/google/gwt/user/client/ui/MenuItem.java \
  com/google/gwt/user/client/ui/SplitLayoutPanel.java \
  com/google/gwt/user/client/ui/SplitPanel.java
  
jar xvf ${SRCBASE}/../lib/gwt/${GWTVER}/gwt-dev.jar \
  com/google/gwt/core/linker/CrossSiteIframeLinker.java \
  com/google/gwt/core/ext/linker/impl/installLocationIframe.js

# apply previous patches
cd ${SRCBASE}/com/google/gwt/user/cellview/client
patch AbstractHeaderOrFooterBuilder.java < AbstractHeaderOrFooterBuilder.java.diff

cd ${SRCBASE}/com/google/gwt/user/client/ui
patch CellPanel.java < CellPanel.java.diff
patch DecoratorPanel.java < DecoratorPanel.java.diff
patch FormPanel.java < FormPanel.java.diff
patch Label.java < Label.java.diff
patch LabelBase.java < LabelBase.java.diff
patch MenuBar.java < MenuBar.java.diff
patch MenuItem.java < MenuItem.java.diff
patch SplitLayoutPanel.java < SplitLayoutPanel.java.diff
patch SplitPanel.java < SplitPanel.java.diff

cd ${SRCBASE}/com/google/gwt/core/ext/linker/impl
patch installLocationIframe.js < installLocationIframe.js.diff

cd ${SRCBASE}/com/google/gwt/core/linker
patch CrossSiteIframeLinker.java < CrossSiteIframeLinker.java.diff
