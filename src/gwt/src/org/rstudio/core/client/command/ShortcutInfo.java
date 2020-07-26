/*
 * ShortcutInfo.java
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

import java.util.ArrayList;
import java.util.List;

public class ShortcutInfo
{
   public ShortcutInfo (KeyboardShortcut shortcut, AppCommand command)
   {
      shortcuts_ = new ArrayList<String>();
      description_ = shortcut.getTitle().length() > 0 ?
                        shortcut.getTitle() :
                        command != null ?
                           command.getMenuLabel(false) :
                           "";
      groupName_ = shortcut.getGroupName();
      isActive_ = command != null ?
                     (command.isEnabled() && command.isVisible()) :
                     true;
      order_ = shortcut.getOrder();
      disableModes_ = shortcut.getDisableModes();
      addShortcut(shortcut);
   }

   public String getDescription()
   {
      return description_;
   }

   public List<String> getShortcuts()
   {
      return shortcuts_;
   }

   public void addShortcut(KeyboardShortcut shortcut)
   {
      shortcuts_.clear();
      shortcuts_.add(shortcut.toString(true));
   }

   public String getGroupName()
   {
      return groupName_;
   }

   public boolean isActive()
   {
      return isActive_;
   }

   public int getOrder()
   {
      return order_;
   }

   public int getDisableModes()
   {
      return disableModes_;
   }

   private List<String> shortcuts_;
   private String description_;
   private String groupName_;
   private boolean isActive_;
   private int order_;
   private int disableModes_;
}
