/*
 * AppCommandPaletteEntry.java
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
package org.rstudio.studio.client.palette.ui;

import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppCommand.Context;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.core.client.command.KeySequence;

/**
 * AppCommandPaletteEntry is a widget that represents an AppCommand in RStudio's
 * command palette.
 */
public class AppCommandPaletteEntry extends CommandPaletteCommand
{
   public AppCommandPaletteEntry(AppCommand command, List<KeySequence> keys)
   {
      super(keys);
      label_ = command.getLabel();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getButtonLabel();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getDesc();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getMenuLabel(false);
      if (StringUtil.isNullOrEmpty(label_))
         label_ = "";
      command_ = command;
      initialize();
   }
   
   public String getLabel()
   {
      return label_;
   }
   
   public void invoke()
   {
      GlobalDisplay display = RStudioGinjector.INSTANCE.getGlobalDisplay();
      if (!command_.isVisible())
      {
         // This isn't currently likely since we hide commands that aren't
         // visible.
         display.showErrorMessage("Command Not Available", 
               "The command '" + getLabel() + "' is not currently available.");
      }
      else if (!command_.isEnabled() || !command_.hasCommandHandlers())
      {
         // Don't attempt to execute disabled commands. Treat command with no
         // handlers as disabled (nothing will happen if we run them except a
         // runtime exception)
         display.showErrorMessage("Command Disabled", 
               "The command '" + getLabel() + "' cannot be used right now. " +
               "It may be unavailable in this project, file, or view.");
      }
      else
      {
         // Regular command execution attempt; we still wrap this in a try/catch
         // so that if anything goes haywire during execution we can tell the user
         // about it.
         try
         {
            command_.execute();
         }
         catch(Exception e)
         {
            display.showErrorMessage("Command Execution Failed", 
                  "The command '" + getLabel() + "' could not be executed.\n\n" +
                  StringUtil.notNull(e.getMessage()));
            Debug.logException(e);
         }
      }
   }
   
   @Override
   public String getId()
   {
      return command_.getId();
   }

   @Override
   public String getContext()
   {
      // Get the context of this command (e.g. "Workbench", "VCS", "Help")
      Context context = command_.getContext();

      // Most commands are "Workbench" commands (they aren't scoped to a
      // particular feature and can be executed at any time.) To reduce visual
      // clutter and repetitions, we don't show this context tag; all commands
      // are implicitly global unless they have a more specific tag.
      if (context == Context.Workbench)
      {
         return "";
      }

      return context.toString();
   }

   @Override
   public boolean enabled()
   {
      // Ensure the command is enabled *and* has handlers. Generally commands
      // should become invisible or disabled when unavailable, but they also
      // become unavailable when they have no listeners.
      return command_.isEnabled() && command_.hasCommandHandlers();
   }

   private String label_;
   private final AppCommand command_;
}
