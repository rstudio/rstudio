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
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.projects.ProjectMRUList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class ProjectPopupMenu extends ToolbarPopupMenu
{
   public ProjectPopupMenu(SessionInfo sessionInfo, Commands commands)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
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

   interface Resources extends ClientBundle
   {
      ImageResource projectMenu();
   }
   
   private static final Resources RESOURCES =  
                              (Resources) GWT.create(Resources.class);
   private final String activeProjectFile_;
   private ToolbarButton toolbarButton_ = null;
   private ProjectMRUList mruList_ ;
}
