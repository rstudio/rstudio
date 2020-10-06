/*
 * ThemeStyles.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

   String visuallyHidden();
   String tabLayout();
   String tabLayoutLeft();
   String rstheme_tabLayoutCenter();
   String tabLayoutRight();
   String dirtyTab();
   String dirtyTabIndicator();
   String docTabLabel();

   String toolbar();
   String rstheme_secondaryToolbar();
   String secondaryToolbarPanel();
   String globalToolbar();
   String desktopGlobalToolbar();
   String webGlobalToolbar();
   String webHeaderBarCommandsProjectMenu();
   String toolbarButton();
   String noLabel();
   String popupButton();
   String toolbarButtonPushed();
   String emptyProjectMenu();
   String menuSubheader();
   String menuItemSubtitle();

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
   String rstheme_center();

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

   String filterMatch();

   String odd();

   String showFile();
   String showFileFixed();
   String showFilePreFixed();

   String fileUploadPanel();
   String fileUploadField();
   String fileUploadLabel();
   String fileUploadTipLabel();

   String fileList();

   String locatorPanel();

   String multiPodUtilityArea();
   String rstheme_multiPodUtilityTabArea();

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
   String focusedWindowFrameObject();
   String rstheme_minimizedWindowObject();
   String windowFrameWidget();

   String consoleOnlyWindowFrame();
   String consoleWidgetLayout();
   String consoleHeaderLayout();
   String consoleMinimizeLayout();
   String consoleMaximizeLayout();

   String tallerToolbarWrapper();
   String rstheme_toolbarWrapper();
   String webGlobalToolbarWrapper();
   String desktopGlobalToolbarWrapper();

   String progressPanel();

   String clearBuildButton();
   String consoleClearButton();
   String terminalClearButton();
   String refreshToolbarButton();

   String dataTableColumnWidget();

   String tabIcon();

   String menuCheckable();

   String noLogo();

   String launcherJobRunButton();
}
