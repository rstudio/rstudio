/*
 * AppCommandPaletteItem.java
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
package org.rstudio.studio.client.palette;

import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.AppCommand.Context;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.palette.ui.AppCommandPaletteEntry;

public class AppCommandPaletteItem extends BasePaletteItem<AppCommandPaletteEntry>
{
   public AppCommandPaletteItem(AppCommand command, List<KeySequence> keys)
   {
      command_ = command;
      keys_ = keys;
      label_ = command.getLabel();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getButtonLabel();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getDesc();
      if (StringUtil.isNullOrEmpty(label_))
         label_ = command.getMenuLabel(false);
      if (StringUtil.isNullOrEmpty(label_))
         label_ = "";
   }

   @Override
   public AppCommandPaletteEntry createWidget()
   {
      return new AppCommandPaletteEntry(command_, label_, keys_, this);
   }

   @Override
   public void invoke(InvocationSource source)
   {
      GlobalDisplay display = RStudioGinjector.INSTANCE.getGlobalDisplay();
      if (!command_.isVisible())
      {
         // This isn't currently likely since we hide commands that aren't
         // visible.
         display.showErrorMessage("Command Not Available", 
               "The command '" + label_ + "' is not currently available.");
      }
      else if (!command_.isEnabled() || !command_.hasCommandHandlers())
      {
         // Don't attempt to execute disabled commands. Treat command with no
         // handlers as disabled (nothing will happen if we run them except a
         // runtime exception)
         display.showErrorMessage("Command Disabled", 
               "The command '" + label_ + "' cannot be used right now. " +
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
                  "The command '" + label_ + "' could not be executed.\n\n" +
                  StringUtil.notNull(e.getMessage()));
            Debug.logException(e);
         }
      }
   }
   
   @Override
   public boolean dismissOnInvoke()
   {
      return true;
   }
   
   @Override
   public boolean matchesSearch(String[] keywords)
   {
      String prefix = "";

      // Non-workbench commands can match on context
      if (command_.getContext() != Context.Workbench)
      {
         prefix = command_.getContext().toString() + " ";
      }

      // Matches if the label matches
      return super.labelMatchesSearch(prefix + label_, keywords);
   }

   @Override
   public void setSearchHighlight(String[] keywords)
   {
      widget_.setSearchHighlight(keywords);
   }

   @Override
   public void setSelected(boolean selected)
   {
      widget_.setSelected(selected);
   }

   private final List<KeySequence> keys_;
   private final AppCommand command_;

   private String label_;
}
