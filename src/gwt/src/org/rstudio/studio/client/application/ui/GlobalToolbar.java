/*
 * GlobalToolbar.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchWidget;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;


public class GlobalToolbar extends Toolbar
{
   public GlobalToolbar(Commands commands, 
                        FileTypeCommands fileTypeCommands,
                        EventBus eventBus,
                        WorkbenchServerOperations server)
   {
      super();
      commands_ = commands;
      server_ = server;
      eventBus_ = eventBus;
      ThemeResources res = ThemeResources.INSTANCE;
      addStyleName(res.themeStyles().globalToolbar());
      
      
      // add newSourceDoc command
      ToolbarPopupMenu newMenu = new ToolbarPopupMenu();
      newMenu.addItem(commands.newSourceDoc().createMenuItem(false));
      
      // dynamically add other commands
      ArrayList<FileTypeCommands.CommandWithId> fileNewCommands = 
         fileTypeCommands.commandsWithIds(FileTypeRegistry.R);
      for (FileTypeCommands.CommandWithId cmd : fileNewCommands)
         newMenu.addItem(cmd.command.createMenuItem(false));
      
      // create and add new menu
      StandardIcons icons = StandardIcons.INSTANCE;
      ToolbarButton newButton = new ToolbarButton("",
                                                  icons.stock_new(),
                                                  newMenu);
      addLeftWidget(newButton);
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
   }
   
   public void addProjectTools(SessionInfo sessionInfo)
   {
      if (sessionInfo.isIndexingEnabled())
      {
         searchWidget_ = new CodeSearchWidget();
         
         new CodeSearch(
               new CodeSearch.Display()
               {       
                  @Override
                  public SearchDisplay getSearchBox()
                  {
                     return searchWidget_;
                  }
               },
               server_, 
               eventBus_);
         
         addLeftSeparator();
         addLeftWidget(searchWidget_);
      }
      
      
      ToolbarPopupMenu projectMenu = new ToolbarPopupMenu();
      
      projectMenu.addItem(commands_.newProject().createMenuItem(false));
      projectMenu.addItem(commands_.openProject().createMenuItem(false));
      projectMenu.addSeparator();
      projectMenu.addItem(commands_.projectMru0().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru1().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru2().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru3().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru4().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru5().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru6().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru7().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru8().createMenuItem(false));
      projectMenu.addItem(commands_.projectMru9().createMenuItem(false));
      projectMenu.addSeparator();
      projectMenu.addItem(commands_.closeProject().createMenuItem(false));
      projectMenu.addSeparator();
      projectMenu.addItem(commands_.projectOptions().createMenuItem(false));
      
      String activeProjectFile = sessionInfo.getActiveProjectFile();
      String menuText = activeProjectFile != null ?
        FileSystemItem.createFile(activeProjectFile).getParentPath().getStem() :
        "Project: (None)";
               
      ToolbarButton projectButton = new ToolbarButton(
            menuText, 
            RESOURCES.projectMenu(),
            projectMenu, 
            true);
      
      if (activeProjectFile == null)
      {
         projectButton.addStyleName(
               ThemeResources.INSTANCE.themeStyles().emptyProjectMenu());
      }
    
      addRightWidget(projectButton);
   }

   @Override
   public int getHeight()
   {
      return 27;
   }
     
   private final Commands commands_;
   private final EventBus eventBus_;
   private final WorkbenchServerOperations server_;
   
   private SearchWidget searchWidget_;
   
   interface Resources extends ClientBundle
   {
      ImageResource projectMenu();
   }
   
   private static final Resources RESOURCES =  
                              (Resources) GWT.create(Resources.class);
}
