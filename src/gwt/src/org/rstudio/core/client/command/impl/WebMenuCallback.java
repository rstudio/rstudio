/*
 * WebMenuCallback.java
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
package org.rstudio.core.client.command.impl;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuBar;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.MenuCallback;

import java.util.Stack;

public class WebMenuCallback implements MenuCallback
{
   public void beginMainMenu()
   {
      menuStack_.push(new AppMenuBar(false));
   }

   public void beginMenu(String label)
   {
      label = AppMenuItem.replaceMnemonics(label, "");

      AppMenuBar newMenu = new AppMenuBar(true);
      head().addItem(label, newMenu);
      menuStack_.push(newMenu);
   }

   public void addCommand(String commandId, AppCommand command)
   {
      head().addItem(command.createMenuItem(true));
   }

   public void addSeparator()
   {
      head().addSeparator();
   }

   public void endMenu()
   {
      menuStack_.pop();
   }

   public void endMainMenu()
   {
      result_ = menuStack_.pop();
   }

   public AppMenuBar getMenu()
   {
      return result_;
   }

   private AppMenuBar head()
   {
      return menuStack_.peek();
   }

   private final Stack<AppMenuBar> menuStack_ = new Stack<AppMenuBar>();
   private AppMenuBar result_;
}
