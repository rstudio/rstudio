/*
 * CommandPaletteLauncher.java
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

package org.rstudio.studio.client.application.ui;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CommandPaletteLauncher implements CommandPalette.Host
{
   public interface Binder
          extends CommandBinder<Commands, CommandPaletteLauncher> {}
   
   @Inject 
   public CommandPaletteLauncher(Commands commands,
         Binder binder)
   {
      binder.bind(commands, this);
      commands_ = commands;
   }
    
   @Handler
   public void onShowCommandPalette()
   {
      if (panel_ != null)
      {
         panel_.show();
         palette_.focus();
         return;
      }
      
      palette_ = new CommandPalette(commands_, this);
      
      panel_ = new PopupPanel(true, true);
      panel_.add(palette_);
      panel_.show();
      panel_.center();
      palette_.focus();
   }

   @Override
   public void dismiss()
   {
      panel_.hide();
   }
   
   private PopupPanel panel_;
   private CommandPalette palette_;
   private final Commands commands_;
}