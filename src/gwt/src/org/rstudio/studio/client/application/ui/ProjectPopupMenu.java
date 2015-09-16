/*
 * ProjectPopupMenu.java
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

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.projects.ProjectMRUList;
import org.rstudio.studio.client.projects.events.OpenProjectFileEvent;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.SharedProjectDetails;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class ProjectPopupMenu extends ToolbarPopupMenu
{
   public ProjectPopupMenu(SessionInfo sessionInfo, Commands commands)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      commands_ = commands;
      
      activeProjectFile_ = sessionInfo.getActiveProjectFile();
   }
   
   @Inject
   void initialize(ProjectMRUList mruList,
                   ProjectsServerOperations server,
                   EventBus events,
                   Session session)
   {
      server_ = server;
      events_ = events;
      mruList_ = mruList;
      allowSharedProjects_ = 
            session.getSessionInfo().getAllowOpenSharedProjects();
   }
   
   public ToolbarButton getToolbarButton()
   {
      if (toolbarButton_ == null)
      {
         String buttonText = activeProjectFile_ != null ?
                  mruList_.getQualifiedLabel(activeProjectFile_) :
                  "Project: (None)";
          
         toolbarButton_ = new ToolbarButton(
                buttonText, 
                RESOURCES.projectMenu(),
                this, 
                true);
          
         if (activeProjectFile_ != null)
         {
            toolbarButton_.setTitle(activeProjectFile_);
          
            // also set the doc title so the browser tab carries the project
            if (!Desktop.isDesktop())
               Document.get().setTitle("RStudio - " + buttonText);
         }
        
          if (activeProjectFile_ == null)
          {
             toolbarButton_.addStyleName(
                   ThemeResources.INSTANCE.themeStyles().emptyProjectMenu());
          }
      }
       
       return toolbarButton_;
   }
   
   @Override
   protected ToolbarMenuBar createMenuBar()
   {
      return new ProjectsPopupMenuBar();
   }
   
   private class ProjectsPopupMenuBar extends ToolbarMenuBar
   {
      public ProjectsPopupMenuBar()
      {
         super(true);
      }
      
      @Override
      public void onBrowserEvent(Event event) {
        Element element = DOM.eventGetTarget(event);
        switch (DOM.eventGetType(event)) {
          case Event.ONCLICK: {
             if (element.getClassName().equals(ThemeStyles.INSTANCE.menuRightImage()))
                ProjectMRUList.setOpenInNewWindow(true);
          }
        }
        super.onBrowserEvent(event);
      }
   }
   
   @Override
   public void getDynamicPopupMenu(final DynamicPopupMenuCallback callback)
   {
      // truncate the MRU list size for smaller client heights
      if (Window.getClientHeight() < 700)
      {
         commands_.projectMru10().setVisible(false);
         commands_.projectMru11().setVisible(false);
         commands_.projectMru12().setVisible(false);
         commands_.projectMru13().setVisible(false);
         commands_.projectMru14().setVisible(false);
      }
      
      ProjectMRUList.setOpenInNewWindow(false);
      if (allowSharedProjects_)
      {
         // if shared projects are on, check for them every time the user drops
         // the menu 
         server_.getSharedProjects(MAX_SHARED_PROJECTS, 
               new ServerRequestCallback<JsArray<SharedProjectDetails>>()
         {
            @Override
            public void onResponseReceived(JsArray<SharedProjectDetails> result)
            {
               rebuildMenu(result, callback);
            }

            @Override
            public void onError(ServerError error)
            {
               // if we can't get the shared projects, we can at least show
               // the rest of the menu
               rebuildMenu(null, callback);
            }
         });
      }
      else
      {
         // shared projects are off; build the menu only if it isn't built
         // already
         if (getItemCount() == 0)
            rebuildMenu(null, callback);
         else
            callback.onPopupMenu(this);
      }
   }

   interface Resources extends ClientBundle
   {
      ImageResource projectMenu();
   }
   
   private void rebuildMenu(final JsArray<SharedProjectDetails> sharedProjects,
         DynamicPopupMenuCallback callback)
   {
      clearItems();
      boolean hasSharedProjects = sharedProjects != null && 
                                  sharedProjects.length() > 0;

      addItem(commands_.newProject().createMenuItem(false));
      addSeparator();
      addItem(commands_.openProject().createMenuItem(false));
      addItem(commands_.openProjectInNewWindow().createMenuItem(false));
      addItem(commands_.shareProject().createMenuItem(false));
      addItem(commands_.closeProject().createMenuItem(false));
      if (hasSharedProjects)
         addSeparator("My Projects"); 
      else
         addSeparator();
      addItem(commands_.projectMru0().createMenuItem(false));
      addItem(commands_.projectMru1().createMenuItem(false));
      addItem(commands_.projectMru2().createMenuItem(false));
      addItem(commands_.projectMru3().createMenuItem(false));
      addItem(commands_.projectMru4().createMenuItem(false));
      addItem(commands_.projectMru5().createMenuItem(false));
      addItem(commands_.projectMru6().createMenuItem(false));
      addItem(commands_.projectMru7().createMenuItem(false));
      addItem(commands_.projectMru8().createMenuItem(false));
      addItem(commands_.projectMru9().createMenuItem(false));
      addItem(commands_.projectMru10().createMenuItem(false));
      addItem(commands_.projectMru11().createMenuItem(false));
      addItem(commands_.projectMru12().createMenuItem(false));
      addItem(commands_.projectMru13().createMenuItem(false));
      addItem(commands_.projectMru14().createMenuItem(false));
      if (hasSharedProjects)
      {
         addSeparator("Shared with Me"); 
         for (int i = 0; i < sharedProjects.length(); i ++)
         {
            final SharedProjectDetails details = sharedProjects.get(i);

            String menuHtml = AppCommand.formatMenuLabel(
                  null, details.getName(), false, null, 
                  commands_.openHtmlExternal().getImageResource(), 
                  ProjectMRUList.NEW_SESSION_DESC);
            addItem(new MenuItem(menuHtml, true,
                  new Scheduler.ScheduledCommand()
                  {
                     @Override
                     public void execute()
                     {
                        ProjectMRUList.openProjectFromMru(events_, 
                              details.getProjectFile());
                     }
                  }));
         }
      }

      addSeparator();
      addItem(commands_.clearRecentProjects().createMenuItem(false));
      addSeparator();
      addItem(commands_.projectOptions().createMenuItem(false));
      
      callback.onPopupMenu(this);
   }

   private static final Resources RESOURCES =  
                              (Resources) GWT.create(Resources.class);
   private static final int MAX_SHARED_PROJECTS = 5;
   private final String activeProjectFile_;
   private ToolbarButton toolbarButton_ = null;

   private ProjectMRUList mruList_;
   private Commands commands_;
   private EventBus events_;
   private ProjectsServerOperations server_;
   private boolean allowSharedProjects_ = false;
}
