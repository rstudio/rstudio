/*
 * ThemeStyles.java
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

import com.google.gwt.resources.client.CssResource;

/**
 * .docTab
 *    %table.docTable
 *       %tr
 *          %td.docTabLayoutLeft
 *          %td.docTabLayoutCenter
 *          %td.docTabLayoutRight
 */
public interface ThemeStyles extends CssResource
{
   public static ThemeStyles INSTANCE = ThemeResources.INSTANCE.themeStyles();

   String NW();
   String N();
   String NE();
   String W();
   String C();
   String E();
   String SW();
   String S();
   String SE();

   String windowframe();

   String primaryWindowFrameHeader();
   String title();
   String subtitle();

   String docTabPanel();
   String docTabIcon();
   String docMenuScroll();
          
   String closeTabButton();

   String fixedWidthFont();
   
   String tabLayout();
   String tabLayoutLeft();
   String tabLayoutCenter();
   String tabLayoutRight();
   String dirtyTab();
   String dirtyTabIndicator();
   String docTabLabel();

   String toolbar();
   String secondaryToolbar();
   String secondaryToolbarPanel();
   String globalToolbar();
   String desktopGlobalToolbar();
   String webGlobalToolbar();
   String webHeaderBarCommandsProjectMenu();
   String toolbarButton();
   String noLabel();
   String toolbarButtonPushed();
   String emptyProjectMenu();
   String menuSubheader();

   String menuRightImage();
   
   String scrollableMenuBar();

   String moduleTabPanel();
   String minimized();
          
   String firstTabSelected();

   String toolbarSeparator();

   String toolbarButtonMenu();
   String toolbarButtonMenuOnly();
   String toolbarButtonLabel();
   String toolbarButtonInfoLabel();
   String toolbarButtonLeftImage();
   String toolbarButtonRightImage();
   String toolbarFileLabel();
   
   String toolbarButtonLatched();
   String toolbarButtonLatchable();
   
   String windowFrameToolbarButton();

   String statusBarMenu();

   String maximize();
   String minimize();

   String left();
   String right();
   String center();

   String minimizedWindow();

   String header();
   String mainMenu();

   String miniToolbar();

   String search();
   String searchMagGlass();
   String searchBoxContainer();
   String searchBoxContainer2();
   String searchBox();
   String clearSearch();

   String dialogBottomPanel();
   
   String dialogMessage();
   String sessionAbendMessage();
   String applicationHeaderStrong();
   
   String environmentHierarchicalCol();
   String environmentDataFrameCol();
   String environmentFunctionCol();

   String odd();
   
   String showFile();
   String showFileFixed();
   
   String fileUploadPanel();
   String fileUploadField();
   String fileUploadTipLabel();
   
   String fileList();

   String locatorPanel();

   String multiPodUtilityArea();

   String tabOverflowPopup();   
   
   String miniDialogPopupPanel();
   String miniDialogContainer();
   String miniDialogCaption();
   String miniDialogTools();
   
   String selectWidget();
   String textBoxWithButton();
   
   String selectableText();
   String forceMacScrollbars();
   
   String adornedText();
   
   String fullscreenCaptionIcon();
   String fullscreenCaptionLabel();
   
   String presentationNavigatorLabel();
   
   String notResizable();
   
   String dialogTabPanel();
   
   String handCursor();
   
   String borderedIFrame();
   
   String toolbarInfoLabel();
   
   String displayNone();
   String logoAnchor();

   String windowFrameObject();
   String minimizedWindowObject();
   String windowFrameWidget();

   String consoleOnlyWindowFrame();
   String consoleWidgetLayout();
   String consoleHeaderLayout();
   String consoleMinimizeLayout();
   String consoleMaximizeLayout();
}
