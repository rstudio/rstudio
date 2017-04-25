/*
 * ThemeResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.theme.res;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;

public interface ThemeResources extends ClientBundle
{
   public static final ThemeResources INSTANCE = GWT.create(ThemeResources.class);

   ThemeStyles themeStyles();

   @Source("dialogTopLeft.png")
   DataResource dialogTopLeft();
   @Source("dialogTop.png")
   DataResource dialogTop();
   @Source("dialogTopRight.png")
   DataResource dialogTopRight();
   @Source("dialogLeft.png")
   DataResource dialogLeft();
   @Source("dialogRight.png")
   DataResource dialogRight();
   @Source("dialogBottomLeft.png")
   DataResource dialogBottomLeft();
   @Source("dialogBottom.png")
   DataResource dialogBottom();
   @Source("dialogBottomRight.png")
   DataResource dialogBottomRight();

   @Source("podTopLeft.png")
   DataResource podTopLeft();
   @Source("podTop.png")
   DataResource podTop();
   @Source("podTopRight.png")
   DataResource podTopRight();
   @Source("podLeft.png")
   DataResource podLeft();
   @Source("podRight.png")
   DataResource podRight();
   @Source("podBottomLeft.png")
   DataResource podBottomLeft();
   @Source("podBottom.png")
   DataResource podBottom();
   @Source("podBottomRight.png")
   DataResource podBottomRight();

   @Source("verticalHandle.png")
   DataResource verticalHandle();

   @Source("horizontalHandle.png")
   DataResource horizontalHandle();

   @Source("rstudio_2x.png")
   ImageResource rstudio2x();

   @Source("rstudio_small_2x.png")
   ImageResource rstudio_small2x();
   
   @Source("rstudio_home_2x.png")
   ImageResource rstudio_home2x();

   @Source("rstudio_home_small_2x.png")
   ImageResource rstudio_home_small2x();
   
   @Source("backgroundGradient.png")
   DataResource backgroundGradient();

   ImageResource activeDocTabLeft();
   ImageResource activeDocTabRight();
   @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
   ImageResource activeDocTabTile();
   ImageResource docTabLeft();
   ImageResource docTabRight();
   @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
   ImageResource docTabTile();
   @Source("tabBackground.png")
   DataResource tabBackground();

   @ImageOptions(repeatStyle = RepeatStyle.Both)
   ImageResource clear();

   @Source("toolbarBackground.png")
   DataResource toolbarBackground();
   @Source("toolbarBackground2.png")
   DataResource toolbarBackground2();
   @Source("desktopGlobalToolbarBackground.png")
   DataResource desktopGlobalToolbarBackground();
   @Source("webGlobalToolbarLeft.png")
   DataResource webGlobalToolbarLeft();
   @Source("webGlobalToolbarRight.png")
   DataResource webGlobalToolbarRight();
   @Source("webGlobalToolbarTile.png")
   DataResource webGlobalToolbarTile();

   @Source("multiPodActiveTabLeft.png")
   DataResource multiPodActiveTabLeft();
   @Source("multiPodActiveTabRight.png")
   DataResource multiPodActiveTabRight();
   @Source("multiPodActiveTabTile.png")
   DataResource multiPodActiveTabTile();
   @Source("multiPodTabLeft.png")
   DataResource multiPodTabLeft();
   @Source("multiPodTabRight.png")
   DataResource multiPodTabRight();
   @Source("multiPodTop.png")
   DataResource multiPodTop();
   @Source("multiPodTopFade.png")
   DataResource multiPodTopFade();

   ImageResource menuBevel();

   @Source("closeTabSelected_2x.png")
   ImageResource closeTab2x();

   @Source("busyTab.gif")
   ImageResource busyTab();

   @Source("closeDialog_2x.png")
   ImageResource closeDialog2x();
  
   ImageResource toolbarSeparator();

   @Source("menuDownArrow_2x.png")
   ImageResource menuDownArrow2x();

   @Source("linkDownArrow_2x.png")
   ImageResource linkDownArrow2x();

   @Source("maximize_2x.png")
   DataResource maximize2x();

   @Source("maximizeSelected_2x.png")
   DataResource maximizeSelected2x();

   @Source("minimize_2x.png")
   DataResource minimize2x();

   @Source("minimizeSelected_2x.png")
   DataResource minimizeSelected2x();

   @Source("restore_2x.png")
   DataResource restore2x();

   @Source("restoreSelected_2x.png")
   DataResource restoreSelected2x();

   @Source("podMinimizedLeft.png")
   DataResource podMinimizedLeft();
   @Source("podMinimizedTile.png")
   DataResource podMinimizedTile();
   @Source("podMinimizedRight.png")
   DataResource podMinimizedRight();

   @Source("searchFieldLeft.png")
   DataResource searchFieldLeft();
   @Source("searchFieldTile.png")
   DataResource searchFieldTile();
   @Source("searchFieldRight.png")
   DataResource searchFieldRight();

   @Source("clearSearch_2x.png")
   ImageResource clearSearch2x();

   @Source("workspaceSectionHeaderTile.png")
   DataResource workspaceSectionHeaderTile();

   @Source("zoomDataset_2x.png")
   ImageResource zoomDataset2x();

   @Source("viewFunctionCode_2x.png")
   ImageResource viewFunctionCode2x();
   
   @Source("inspectObject_2x.png")
   ImageResource inspectObject2x();

   @Source("inlineEditIcon_2x.png")
   DataResource inlineEditIcon2x();

   @Source("inlineDeleteIcon_2x.png")
   DataResource inlineDeleteIcon2x();

   @Source("paneLayoutIcon_2x.png")
   ImageResource paneLayoutIcon2x();

   @Source("smallMagGlassIcon_2x.png")
   ImageResource smallMagGlassIcon2x();

   @Source("dropDownArrow_2x.png")
   ImageResource dropDownArrow2x();

   @Source("mediumDropDownArrow_2x.png")
   ImageResource mediumDropDownArrow2x();

   @Source("chevron_2x.png")
   ImageResource chevron2x();

   @Source("help_2x.png")
   ImageResource help2x();

   @Source("infoSmall_2x.png")
   ImageResource infoSmall2x();

   @Source("warningSmall_2x.png")
   ImageResource warningSmall2x();

   @Source("errorSmall_2x.png")
   ImageResource errorSmall2x();

   @Source("syntaxInfo_2x.png")
   ImageResource syntaxInfo2x();

   @Source("syntaxWarning_2x.png")
   ImageResource syntaxWarning2x();

   @Source("syntaxError_2x.png")
   ImageResource syntaxError2x();

   @Source("syntaxInfoDark_2x.png")
   ImageResource syntaxInfoDark2x();

   @Source("syntaxWarningDark_2x.png")
   ImageResource syntaxWarningDark2x();

   @Source("syntaxErrorDark_2x.png")
   ImageResource syntaxErrorDark();

   @Source("syntaxErrorDark_2x.png")
   ImageResource syntaxErrorDark2x();
   
   @Source("codeTransform_2x.png")
   ImageResource codeTransform2x();

   @Source("closeChevron_2x.png")
   ImageResource closeChevron2x();
   
   @Source("removePackage_2x.png")
   ImageResource removePackage2x();
   
   @Source("newsButton_2x.png")
   ImageResource newsButton2x();
   
   @Source("activeBreakpoint_2x.png")
   DataResource activeBreakpoint2x();

   @Source("inactiveBreakpoint_2x.png")
   DataResource inactiveBreakpoint2x();

   @Source("pendingBreakpoint_2x.png")
   DataResource pendingBreakpoint2x();

   @Source("executingLine_2x.png")
   DataResource executingLine2x();
   
   @Source("macCheck_2x.png")
   DataResource macCheck2x();

   @Source("radioButtonOn_2x.png")
   DataResource radioButtonOn2x();

   @Source("handCursor_2x.png")
   DataResource handCursor2x();
   
   @Source("executeChunk_2x.png")
   ImageResource executeChunk2x();
   
   ImageResource checkboxOff();
   ImageResource checkboxOn();

   @Source("checkboxTri_2x.png")
   ImageResource checkboxTri2x();
   
   @Source("menuCheck_2x.png")
   ImageResource menuCheck2x();
   
   @Source("user_2x.png")
   ImageResource user();
}
