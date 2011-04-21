/*
 * ToolbarPopupMenu.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.BaseMenuBar;

public class ToolbarPopupMenu extends ThemedPopupPanel
{
   public ToolbarPopupMenu()
   {
      super(true);
      menuBar_ = new ToolbarMenuBar(true);
      menuBar_.setVisible(true);
      add(menuBar_);
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      menuBar_.selectItem(null);
   }

   public void addItem(MenuItem menuItem)
   {
      Command command = menuItem.getCommand();
      if (command == null && menuItem instanceof AppMenuItem)
         command = ((AppMenuItem)menuItem).getCommand(true);
      if (command != null)
         menuItem.setCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.addItem(menuItem);
   }
   
   public void insertItem(MenuItem menuItem, int beforeIndex)
   {
      Command command = menuItem.getCommand() ;
      if (command != null)
         menuItem.setCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.insertItem(menuItem, beforeIndex) ;
   }
   
   public void removeItem(MenuItem menuItem)
   {
      menuBar_.removeItem(menuItem) ;
   }
   
   public boolean containsItem(MenuItem menuItem)
   {
      return menuBar_.getItemIndex(menuItem) >= 0 ;
   }
   
   public void clearItems()
   {
      menuBar_.clearItems() ;
   }
   
   public void addSeparator()
   {
      menuBar_.addSeparator();
   }
   
   public int getItemCount()
   {
      return menuBar_.getItemCount() ;
   }
   
   private class ToolbarPopupMenuCommand implements Command
   {
      public ToolbarPopupMenuCommand(Command coreCommand)
      {
         coreCommand_ = coreCommand;
      }
      public void execute()
      {
         Scheduler.get().scheduleDeferred(coreCommand_);
         hide();
      }
   
      private Command coreCommand_;
   }
   
   private class ToolbarMenuBar extends BaseMenuBar
   {
      public ToolbarMenuBar(boolean vertical)
      {
         super(vertical) ;
      }
      
      public int getItemCount()
      {
         return getItems().size() ;
      }
   }
   
   private ToolbarMenuBar menuBar_ ;
}
