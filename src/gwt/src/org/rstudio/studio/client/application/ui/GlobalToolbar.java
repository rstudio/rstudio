/*
 * GlobalToolbar.java
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
package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FocusContext;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.ui.addins.AddinsToolbarButton;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Provider;


public class GlobalToolbar extends Toolbar
{
   public GlobalToolbar(Commands commands,
                        Provider<CodeSearch> pCodeSearch)
   {
      super("Main");

      commands_ = commands;
      pCodeSearch_ = pCodeSearch;
      ThemeResources res = ThemeResources.INSTANCE;
      addStyleName(res.themeStyles().globalToolbar());


      // add new source doc commands
      newMenu_ = new ToolbarPopupMenu();
      newMenu_.addItem(commands.newSourceDoc().createMenuItem(false));
      newMenu_.addSeparator();
      newMenu_.addItem(commands.newRNotebook().createMenuItem(false));
      newMenu_.addSeparator();
      newMenu_.addItem(commands.newRMarkdownDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRShinyApp().createMenuItem(false));
      newMenu_.addItem(commands.newRPlumberDoc().createMenuItem(false));
      newMenu_.addSeparator();
      newMenu_.addItem(commands.newTextDoc().createMenuItem(false));
      newMenu_.addItem(commands.newCppDoc().createMenuItem(false));
      newMenu_.addItem(commands.newPythonDoc().createMenuItem(false));
      newMenu_.addItem(commands.newSqlDoc().createMenuItem(false));
      newMenu_.addItem(commands.newStanDoc().createMenuItem(false));
      newMenu_.addItem(commands.newD3Doc().createMenuItem(false));
      newMenu_.addSeparator();
      newMenu_.addItem(commands.newSweaveDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRHTMLDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRPresentationDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRDocumentationDoc().createMenuItem(false));

      // create and add new menu
      StandardIcons icons = StandardIcons.INSTANCE;
      newButton_ = new ToolbarMenuButton(ToolbarButton.NoText,
                                                          "New File",
                                                          new ImageResource2x(icons.stock_new2x()),
                                                          newMenu_);
      ElementIds.assignElementId(newButton_, ElementIds.NEW_FILE_MENUBUTTON);
      addLeftWidget(newButton_);
      addLeftSeparator();

      addLeftWidget(commands.newProject().createToolbarButton());
      addLeftSeparator();

      // open button + mru
      addLeftWidget(commands.openSourceDoc().createToolbarButton());

      ToolbarPopupMenu mruMenu = new ToolbarPopupMenu();
      mruMenu.addItem(commands.mru0().createMenuItem(false));
      mruMenu.addItem(commands.mru1().createMenuItem(false));
      mruMenu.addItem(commands.mru2().createMenuItem(false));
      mruMenu.addItem(commands.mru3().createMenuItem(false));
      mruMenu.addItem(commands.mru4().createMenuItem(false));
      mruMenu.addItem(commands.mru5().createMenuItem(false));
      mruMenu.addItem(commands.mru6().createMenuItem(false));
      mruMenu.addItem(commands.mru7().createMenuItem(false));
      mruMenu.addItem(commands.mru8().createMenuItem(false));
      mruMenu.addItem(commands.mru9().createMenuItem(false));
      mruMenu.addItem(commands.mru10().createMenuItem(false));
      mruMenu.addItem(commands.mru11().createMenuItem(false));
      mruMenu.addItem(commands.mru12().createMenuItem(false));
      mruMenu.addItem(commands.mru13().createMenuItem(false));
      mruMenu.addItem(commands.mru14().createMenuItem(false));
      mruMenu.addSeparator();
      mruMenu.addItem(commands.clearRecentFiles().createMenuItem(false));

      ToolbarMenuButton mruButton = new ToolbarMenuButton(ToolbarButton.NoText,
                                                          "Open recent files",
                                                          mruMenu,
                                                          false);
      ElementIds.assignElementId(mruButton, ElementIds.OPEN_MRU_MENUBUTTON);
      addLeftWidget(mruButton);
      addLeftSeparator();

      addLeftWidget(commands.saveSourceDoc().createToolbarButton());
      addLeftWidget(commands.saveAllSourceDocs().createToolbarButton());
      addLeftSeparator();

      addLeftWidget(commands.printSourceDoc().createToolbarButton());

      addLeftSeparator();
      CodeSearch codeSearch = pCodeSearch_.get();
      codeSearch.setObserver(new CodeSearch.Observer()
      {
         @Override
         public void onCancel()
         {
            // Experimental workaround for crashes observed on El Capitan
            Scheduler.get().scheduleFinally(() -> codeSearchFocusContext_.restore());
         }

         @Override
         public void onCompleted()
         {
            Scheduler.get().scheduleFinally(() -> codeSearchFocusContext_.clear());
         }

         @Override
         public String getCueText()
         {
            return null;
         }
      });

      searchWidget_ = codeSearch.getSearchWidget();
      addLeftWidget(searchWidget_);
   }

   public void completeInitialization(SessionInfo sessionInfo)
   {
      StandardIcons icons = StandardIcons.INSTANCE;

      if (sessionInfo.isVcsEnabled())
      {
         addLeftSeparator();

         ToolbarPopupMenu vcsMenu = new ToolbarPopupMenu();
         vcsMenu.addItem(commands_.vcsFileDiff().createMenuItem(false));
         vcsMenu.addItem(commands_.vcsFileLog().createMenuItem(false));
         vcsMenu.addItem(commands_.vcsFileRevert().createMenuItem(false));
         vcsMenu.addSeparator();
         vcsMenu.addItem(commands_.vcsViewOnGitHub().createMenuItem(false));
         vcsMenu.addItem(commands_.vcsBlameOnGitHub().createMenuItem(false));
         vcsMenu.addSeparator();
         vcsMenu.addItem(commands_.vcsCommit().createMenuItem(false));
         vcsMenu.addSeparator();
         vcsMenu.addItem(commands_.vcsPull().createMenuItem(false));
         vcsMenu.addItem(commands_.vcsCleanup().createMenuItem(false));
         vcsMenu.addItem(commands_.vcsPush().createMenuItem(false));
         vcsMenu.addSeparator();
         vcsMenu.addItem(commands_.vcsShowHistory().createMenuItem(false));
         vcsMenu.addSeparator();
         vcsMenu.addItem(commands_.versionControlProjectSetup().createMenuItem(false));

         ImageResource vcsIcon = null;
         if (StringUtil.equals(sessionInfo.getVcsName(), VCSConstants.GIT_ID))
            vcsIcon = new ImageResource2x(icons.git2x());
         else if (StringUtil.equals(sessionInfo.getVcsName(), VCSConstants.SVN_ID))
            vcsIcon = new ImageResource2x(icons.svn2x());

         ToolbarMenuButton vcsButton = new ToolbarMenuButton(
               ToolbarButton.NoText,
               "Version control",
               vcsIcon,
               vcsMenu);
         ElementIds.assignElementId(vcsButton, ElementIds.VCS_MENUBUTTON);
         addLeftWidget(vcsButton);
      }

      // zoom button
      addLeftSeparator();

      ToolbarPopupMenu paneLayoutMenu = new ToolbarPopupMenu();

      paneLayoutMenu.addItem(commands_.layoutEndZoom().createMenuItem(false));
      paneLayoutMenu.addSeparator();
      paneLayoutMenu.addItem(commands_.layoutConsoleOnLeft().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutConsoleOnRight().createMenuItem(false));
      paneLayoutMenu.addSeparator();
      paneLayoutMenu.addItem(commands_.paneLayout().createMenuItem(false));
      paneLayoutMenu.addSeparator();
      paneLayoutMenu.addItem(commands_.layoutZoomSource().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomConsole().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomHelp().createMenuItem(false));
      paneLayoutMenu.addSeparator();
      paneLayoutMenu.addItem(commands_.layoutZoomHistory().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomFiles().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomPlots().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomPackages().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomEnvironment().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomViewer().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomVcs().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomBuild().createMenuItem(false));
      paneLayoutMenu.addItem(commands_.layoutZoomConnections().createMenuItem(false));

      ImageResource paneLayoutIcon = new ImageResource2x(ThemeResources.INSTANCE.paneLayoutIcon2x());
      ToolbarMenuButton paneLayoutButton = new ToolbarMenuButton(
            ToolbarButton.NoText,
            "Workspace Panes",
            paneLayoutIcon,
            paneLayoutMenu);
      ElementIds.assignElementId(paneLayoutButton, ElementIds.PANELAYOUT_MENUBUTTON);
      addLeftWidget(paneLayoutButton);

      // addins menu
      addLeftWidget(new AddinsToolbarButton());

      // project popup menu
      if (sessionInfo.getAllowFullUI())
      {
         ProjectPopupMenu projectMenu = new ProjectPopupMenu(
               sessionInfo, commands_, ElementIds.PROJECT_MENUBUTTON_TOOLBAR_SUFFIX);
         addRightWidget(projectMenu.getToolbarButton());
      }
   }

   @Override
   public int getHeight()
   {
      return 27;
   }

   public void focusGoToFunction()
   {
      codeSearchFocusContext_.record();
      FocusHelper.setFocusDeferred((CanFocus)searchWidget_);
   }

   public void setFocus()
   {
      Scheduler.get().scheduleDeferred(() ->
      {
         newButton_.setFocus(true);
         A11y.showFocusOutline(newButton_.getElement());
      });
   }

   private final Commands commands_;
   private final ToolbarPopupMenu newMenu_;
   private final ToolbarMenuButton newButton_;
   private final Provider<CodeSearch> pCodeSearch_;
   private final Widget searchWidget_;
   private final FocusContext codeSearchFocusContext_ = new FocusContext();
}
