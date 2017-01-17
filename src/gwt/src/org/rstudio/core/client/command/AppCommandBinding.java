/*
 * AppCommandBinding.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import org.rstudio.core.client.command.AppCommand.Context;
import org.rstudio.core.client.command.KeyMap.CommandBinding;

public class AppCommandBinding implements CommandBinding
{
   public AppCommandBinding(AppCommand command, String disableModes, boolean custom)
   {
      command_ = command;
      disableModes_ = ShortcutManager.parseDisableModes(disableModes);
      custom_ = custom;
   }
   
   @Override
   public String getId()
   {
      return command_.getId();
   }

   @Override
   public void execute()
   {
      command_.executeFromShortcut();
   }

   @Override
   public boolean isEnabled()
   {
      if (!command_.isEnabled())
         return false;
      
      int mode = ShortcutManager.INSTANCE.getEditorMode();
      if ((disableModes_ & mode) != 0)
         return false;
      
      return true;
   }
   
   @Override
   public boolean isUserDefinedBinding()
   {
      return custom_;
   }

   @Override
   public Context getContext()
   {
      return command_.getContext();
   }

   private final AppCommand command_;
   private final int disableModes_;
   private final boolean custom_;
}
