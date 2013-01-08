/*
 * ToolbarPopupMenu.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.BaseMenuBar;

public class ToolbarPopupMenu extends ThemedPopupPanel
{
   public ToolbarPopupMenu()
   {
      super(true);
      add(wrapMenuBar(menuBar_ = createMenuBar()));
   }

   protected ToolbarMenuBar createMenuBar()
   {
      return new ToolbarMenuBar(true);
   }

   protected Widget wrapMenuBar(ToolbarMenuBar toolbarMenuBar)
   {
      return toolbarMenuBar;
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      menuBar_.selectItem(null);
   }

   public void selectItem(MenuItem menuItem)
   {
      menuBar_.selectItem(menuItem);
   }

   public void addItem(MenuItem menuItem)
   {
      ScheduledCommand command = menuItem.getScheduledCommand();
      if (command == null && menuItem instanceof AppMenuItem)
         command = ((AppMenuItem)menuItem).getScheduledCommand(true);
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.addItem(menuItem);
   }
   
   public void insertItem(MenuItem menuItem, int beforeIndex)
   {
     ScheduledCommand command = menuItem.getScheduledCommand() ;
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
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

   public void focus()
   {
      menuBar_.focus();
   }

   private class ToolbarPopupMenuCommand implements ScheduledCommand
   {
      public ToolbarPopupMenuCommand(ScheduledCommand coreCommand)
      {
         coreCommand_ = coreCommand;
      }
      public void execute()
      {
         Scheduler.get().scheduleFinally(coreCommand_);
         hide();
      }
   
      private ScheduledCommand coreCommand_;
   }
   
   protected class ToolbarMenuBar extends BaseMenuBar
   {
      public ToolbarMenuBar(boolean vertical)
      {
         super(vertical) ;
      }

      @Override
      protected void onUnload()
      {
         nativePreviewReg_.removeHandler();
         super.onUnload();
      }

      @Override
      protected void onLoad()
      {
         super.onLoad();
         nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
         {
            public void onPreviewNativeEvent(NativePreviewEvent e)
            {
               if (e.getTypeInt() == Event.ONKEYDOWN)
               {
                  switch (e.getNativeEvent().getKeyCode())
                  {
                     case KeyCodes.KEY_ESCAPE:
                        e.cancel();
                        hide();
                        break;
                     case KeyCodes.KEY_DOWN:
                        e.cancel();
                        moveSelectionDown();
                        break;
                     case KeyCodes.KEY_UP:
                        e.cancel();
                        moveSelectionUp();
                        break;
                     case KeyCodes.KEY_ENTER:
                        e.cancel();
                        final MenuItem menuItem = getSelectedItem();
                        if (menuItem != null)
                        {
                           NativeEvent evt = Document.get().createClickEvent(
                                 0,
                                 0,
                                 0,
                                 0,
                                 0,
                                 false,
                                 false,
                                 false,
                                 false);
                           menuItem.getElement().dispatchEvent(evt);
                        }
                        break;
                  }
               }
            }
         });
      }

      public int getItemCount()
      {
         return getItems().size() ;
      }

      private HandlerRegistration nativePreviewReg_;
   }
   
   protected ToolbarMenuBar menuBar_ ;
}
