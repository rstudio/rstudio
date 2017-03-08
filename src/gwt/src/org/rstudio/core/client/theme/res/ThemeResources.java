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

   ImageResource rstudio();
   ImageResource rstudio_small();
   
   ImageResource rstudio_home();
   ImageResource rstudio_home_small();
   
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

   @Source("closeTabSelected.png")
   ImageResource closeTab();
   @Source("busyTab.gif")
   ImageResource busyTab();

   ImageResource closeDialog();
  
   ImageResource toolbarSeparator();

   ImageResource menuDownArrow();
   ImageResource linkDownArrow();

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

   ImageResource clearSearch();

   @Source("workspaceSectionHeaderTile.png")
   DataResource workspaceSectionHeaderTile();
   ImageResource zoomDataset();
   ImageResource viewFunctionCode();

   @Source("inlineEditIcon.png")
   DataResource inlineEditIcon();

   @Source("inlineDeleteIcon.png")
   DataResource inlineDeleteIcon();

   @Source("paneLayoutIcon_2x.png")
   ImageResource paneLayoutIcon2x();

   ImageResource smallMagGlassIcon();
   ImageResource dropDownArrow();
   ImageResource mediumDropDownArrow();
   ImageResource chevron();

   ImageResource help();
   
   ImageResource infoSmall();
   ImageResource warningSmall();
   ImageResource errorSmall();
   
   ImageResource syntaxInfo();
   ImageResource syntaxWarning();
   ImageResource syntaxError();
   
   ImageResource syntaxInfoDark();
   ImageResource syntaxWarningDark();
   ImageResource syntaxErrorDark();
   
   
   @Source("codeTransform_2x.png")
   ImageResource codeTransform2x();

   ImageResource closeChevron();
   
   ImageResource removePackage();
   
   ImageResource newsButton();
   
   @Source("activeBreakpoint.png")
   DataResource activeBreakpoint();
   @Source("inactiveBreakpoint.png")
   DataResource inactiveBreakpoint();
   @Source("pendingBreakpoint.png")
   DataResource pendingBreakpoint();
   @Source("executingLine.png")
   DataResource executingLine();
   
   @Source("macCheck.png")
   DataResource macCheck();
   @Source("radioButtonOn.png")
   DataResource radioButtonOn();
   
   @Source("handCursor.png")
   DataResource handCursor();
   
   @Source("executeChunk_2x.png")
   ImageResource executeChunk2x();
   
   ImageResource checkboxOff();
   ImageResource checkboxOn();
   ImageResource checkboxTri();
   
   @Source("menuCheck_2x.png")
   ImageResource menuCheck2x();
   
   ImageResource user();
}
