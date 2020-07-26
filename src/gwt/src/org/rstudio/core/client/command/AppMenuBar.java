/*
 * AppMenuBar.java
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
package org.rstudio.core.client.command;

import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;

import java.util.List;

public class AppMenuBar extends BaseMenuBar
{

   public AppMenuBar(boolean vertical)
   {
      super(vertical);
      vertical_ = vertical;
      setFocusOnHoverEnabled(false);
   }

   @Override
   public MenuItem addItem(String text, MenuitemRole role, boolean checked, ScheduledCommand cmd)
   {
      return addItem(text, false, role, checked, cmd);
   }

   @Override
   public MenuItem addItem(String text, boolean asHTML, MenuitemRole role, boolean checked, ScheduledCommand cmd)
   {
      if (vertical_ && !asHTML)
         text = AppCommand.formatMenuLabel(null, text, null);
      return super.addItem(text,
                           true,
                           role,
                           checked,
                           cmd);
   }

   @Override
   public MenuItem addItem(String text, MenuBar popup)
   {
      return addItem(text, false, popup);
   }

   @Override
   public MenuItem addItem(String text, boolean asHTML, MenuBar popup)
   {
      if (!asHTML)
      {
         if (vertical_)
            text = AppCommand.formatMenuLabel(null, text, null);
         else
            text = "<span id=\"" + ElementIds.idFromLabel(text) + "_menu" + "\">" +
                   DomUtils.textToHtml(text) + "</span>";
      }

      return super.addItem(text,
                           true,
                           popup);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      if (++activeMenuCount_ == 2)
      {
         listeners_.fireEvent(new SubMenuVisibleChangedEvent(true));
      }
   }

   @Override
   protected void onUnload()
   {
      if (--activeMenuCount_ == 1)
      {
         listeners_.fireEvent(new SubMenuVisibleChangedEvent(false));
      }
      super.onUnload();
   }

   @Override
   public MenuItem getSelectedItem()
   {
      return super.getSelectedItem();
   }

   @Override
   public List<MenuItem> getItems()
   {
      return super.getItems();
   }

   public static HandlerRegistration addSubMenuVisibleChangedHandler(
         SubMenuVisibleChangedHandler handler)
   {
      return listeners_.addHandler(SubMenuVisibleChangedEvent.TYPE, handler);
   }

   // used to hide the menu bar itself if every item in the menu is a command,
   // and every command is not visible.
   // (consider: with a little work this could be more generic, such that any
   // menu subtree consisting entirely of invisible commands would be hidden,
   // but there are currently no cases where this is necessary.)
   public boolean allInvisibleCmds()
   {
      for (MenuItem item: super.getItems())
      {
         if (item instanceof AppMenuItem)
         {
            if (((AppMenuItem)item).cmdVisible())
               return false;
         }
         else
            return false;
      }
      return true;
   }

   private static final HandlerManager listeners_ = new HandlerManager(null);
   // Usual value is 1, because main menu counts as an active menu
   private static int activeMenuCount_;
   private final boolean vertical_;
}
