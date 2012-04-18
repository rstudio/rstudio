/*
 * ProjectPopupMenu.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public class ProjectPopupMenu extends ToolbarPopupMenu
{
   public ProjectPopupMenu(SessionInfo sessionInfo, Commands commands)
   {
      addItem(commands.newProject().createMenuItem(false));
      addSeparator();
      addItem(commands.openProject().createMenuItem(false));
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
      addItem(commands.closeProject().createMenuItem(false));
      addSeparator();
      addItem(commands.projectOptions().createMenuItem(false));
      
      activeProjectFile_ = sessionInfo.getActiveProjectFile();
     
      
      
   }
   
   public ToolbarButton getToolbarButton()
   {
      if (toolbarButton_ == null)
      {
         String buttonText = activeProjectFile_ != null ?
                  FileSystemItem.createFile(
                        activeProjectFile_).getParentPath().getStem() :
                  "Project: (None)";
          
         toolbarButton_ = new ToolbarButton(
                buttonText, 
                RESOURCES.projectMenu(),
                this, 
                true);
          
         if (activeProjectFile_ != null)
            toolbarButton_.setTitle(activeProjectFile_);
        
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
}
