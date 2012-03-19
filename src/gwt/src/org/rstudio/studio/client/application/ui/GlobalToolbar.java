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

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FocusContext;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Provider;


public class GlobalToolbar extends Toolbar
{
   public GlobalToolbar(Commands commands, 
                        FileTypeCommands fileTypeCommands,
                        EventBus eventBus,
                        Provider<CodeSearch> pCodeSearch)
   {
      super();
      commands_ = commands;
      pCodeSearch_ = pCodeSearch;
      ThemeResources res = ThemeResources.INSTANCE;
      addStyleName(res.themeStyles().globalToolbar());
      
      
      // add new source doc commands
      ToolbarPopupMenu newMenu = new ToolbarPopupMenu();
      newMenu.addItem(commands.newSourceDoc().createMenuItem(false));
      newMenu.addSeparator();
      newMenu.addItem(commands.newSweaveDoc().createMenuItem(false));
           
      // dynamically add other commands
      ArrayList<FileTypeCommands.CommandWithId> fileNewCommands = 
                                       fileTypeCommands.commandsWithIds();
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
      
      addLeftSeparator();
      CodeSearch codeSearch = pCodeSearch_.get();
      codeSearch.setObserver(new CodeSearch.Observer() {     
         @Override
         public void onCancel()
         {
            codeSearchFocusContext_.restore();     
         }
         
         @Override
         public void onCompleted()
         {   
            codeSearchFocusContext_.clear();
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
   
   public void addProjectTools(SessionInfo sessionInfo)
   { 
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
   private final Provider<CodeSearch> pCodeSearch_;
   private final Widget searchWidget_;
   private final FocusContext codeSearchFocusContext_ = new FocusContext();

}
