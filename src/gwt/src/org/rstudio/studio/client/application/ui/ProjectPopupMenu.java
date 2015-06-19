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

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.projects.ProjectMRUList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;

public class ProjectPopupMenu extends ToolbarPopupMenu
{
   public ProjectPopupMenu(SessionInfo sessionInfo, Commands commands)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      commands_ = commands;
      
      addItem(commands.newProject().createMenuItem(false));
      addSeparator();
      addItem(commands.openProject().createMenuItem(false));
      addItem(commands.openProjectInNewWindow().createMenuItem(false));
      addItem(commands.closeProject().createMenuItem(false));
      addSeparator();
      addItem(commands.projectMru0().createMenuItem(false));
      addItem(commands.projectMru1().createMenuItem(false));
      addItem(commands.projectMru2().createMenuItem(false));
      addItem(commands.projectMru3().createMenuItem(false));
      addItem(commands.projectMru4().createMenuItem(false));
      addItem(commands.projectMru5().createMenuItem(false));
      addItem(commands.projectMru6().createMenuItem(false));
      addItem(commands.projectMru7().createMenuItem(false));
      addItem(commands.projectMru8().createMenuItem(false));
      addItem(commands.projectMru9().createMenuItem(false));
      addItem(commands.projectMru10().createMenuItem(false));
      addItem(commands.projectMru11().createMenuItem(false));
      addItem(commands.projectMru12().createMenuItem(false));
      addItem(commands.projectMru13().createMenuItem(false));
      addItem(commands.projectMru14().createMenuItem(false));
      addSeparator();
      addItem(commands.clearRecentProjects().createMenuItem(false));
      addSeparator();
      addItem(commands.projectOptions().createMenuItem(false));
      
      activeProjectFile_ = sessionInfo.getActiveProjectFile();
     
      
      
   }
   
   @Inject
   void initialize(ProjectMRUList mruList)
   {
      mruList_ = mruList;
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
   public void getDynamicPopupMenu(DynamicPopupMenuCallback callback)
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
      callback.onPopupMenu(this);
   }
   

   interface Resources extends ClientBundle
   {
      ImageResource projectMenu();
   }
   
   private static final Resources RESOURCES =  
                              (Resources) GWT.create(Resources.class);
   private final String activeProjectFile_;
   private ToolbarButton toolbarButton_ = null;
   private ProjectMRUList mruList_;
   private final Commands commands_;
}
