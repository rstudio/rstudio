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

   String tabLayout();
   String tabLayoutLeft();
   String tabLayoutCenter();
   String tabLayoutRight();
   String dirtyTab();
   String dirtyTabIndicator();

   String toolbar();
   String secondaryToolbar();
   String globalToolbar();
   String desktopGlobalToolbar();
   String webGlobalToolbar();
   String webHeaderBarCommandsProjectMenu();
   String toolbarButton();
   String noLabel();
   String toolbarButtonPushed();
   String emptyProjectMenu();

   String scrollableMenuBar();

   String moduleTabPanel();
   String minimized();
          
   String firstTabSelected();

   String toolbarSeparator();

   String toolbarButtonMenu();
   String toolbarButtonMenuOnly();
   String toolbarButtonLabel();
   String toolbarButtonLeftImage();
   String toolbarButtonRightImage();
   String toolbarFileLabel();
   
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
          
   String workspace();
   String workspaceSectionHead();
   String workspaceDataRow();
   String workspaceDataFrameRow();
   String scalarEdit();
   String editing();
   String editPending();

   String odd();
   
   String linkDownArrow();
   
   String showFile();
   String showFileFixed();
   
   String fileUploadPanel();
   String fileUploadField();
   String fileUploadTipLabel();
   
   String fileList();
   String parentDirIcon();

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
}
