/*
 * GlobalToolbar.java
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
package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FocusContext;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Provider;


public class GlobalToolbar extends Toolbar
{
   public GlobalToolbar(Commands commands, 
                        EventBus eventBus,
                        Provider<CodeSearch> pCodeSearch)
   {
      super();
      
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
      newMenu_.addSeparator();
      newMenu_.addItem(commands.newTextDoc().createMenuItem(false));
      newMenu_.addItem(commands.newCppDoc().createMenuItem(false));
      newMenu_.addSeparator();
      newMenu_.addItem(commands.newSweaveDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRHTMLDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRPresentationDoc().createMenuItem(false));
      newMenu_.addItem(commands.newRDocumentationDoc().createMenuItem(false));
      
      // create and add new menu
      StandardIcons icons = StandardIcons.INSTANCE;
      ToolbarButton newButton = new ToolbarButton("",
                                                  new ImageResource2x(icons.stock_new2x()),
                                                  newMenu_);
      addLeftWidget(newButton);
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
      
      ToolbarButton mruButton = new ToolbarButton(mruMenu, false);
      mruButton.setTitle("Open recent files");
      addLeftWidget(mruButton);
      addLeftSeparator();
      
      
      addLeftWidget(commands.saveSourceDoc().createToolbarButton());
      addLeftWidget(commands.saveAllSourceDocs().createToolbarButton());
      addLeftSeparator();
      
      addLeftWidget(commands.printSourceDoc().createToolbarButton());
      
      addLeftSeparator();
      CodeSearch codeSearch = pCodeSearch_.get();
      codeSearch.setObserver(new CodeSearch.Observer() {     
         @Override
         public void onCancel()
         {
            // Experimental workaround for crashes observed on El Capitan
            Scheduler.get().scheduleFinally(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  codeSearchFocusContext_.restore();
               }
            });
         }
         
         @Override
         public void onCompleted()
         {
            Scheduler.get().scheduleFinally(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  codeSearchFocusContext_.clear();
               }
            });
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
         if (sessionInfo.getVcsName().equals(VCSConstants.GIT_ID))
            vcsIcon = new ImageResource2x(icons.git2x());
         else if (sessionInfo.getVcsName().equals(VCSConstants.SVN_ID))
            vcsIcon = new ImageResource2x(icons.svn2x());
         
         ToolbarButton vcsButton = new ToolbarButton(
               null,
               vcsIcon, 
               vcsMenu);
         vcsButton.setTitle("Version control");
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
      ToolbarButton paneLayoutButton = new ToolbarButton(
            null,
            paneLayoutIcon,
            paneLayoutMenu);
      paneLayoutButton.setTitle("Workspace Panes");
      
      addLeftWidget(paneLayoutButton);
      
      // addins menu
      ToolbarPopupMenu addinsMenu = new ToolbarPopupMenu();
      addinsMenu.addItem(commands_.addinsMru0().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru1().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru2().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru3().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru4().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru5().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru6().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru7().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru8().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru9().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru10().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru11().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru12().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru13().createMenuItem(false));
      addinsMenu.addItem(commands_.addinsMru14().createMenuItem(false));
      addinsMenu.addSeparator();
      addinsMenu.addItem(commands_.browseAddins().createMenuItem(false));
      addLeftSeparator();
      ToolbarButton addinsButton = new ToolbarButton(
           "Addins", CoreResources.INSTANCE.iconEmpty(), addinsMenu);
      addLeftWidget(addinsButton);
      
      
      
      // project popup menu
      ProjectPopupMenu projectMenu = new ProjectPopupMenu(sessionInfo,
                                                          commands_);
      addRightWidget(projectMenu.getToolbarButton());
      
     
      
      
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
     
   private final Commands commands_;
   private final ToolbarPopupMenu newMenu_;
   private final Provider<CodeSearch> pCodeSearch_;
   private final Widget searchWidget_;
   private final FocusContext codeSearchFocusContext_ = new FocusContext();
}
