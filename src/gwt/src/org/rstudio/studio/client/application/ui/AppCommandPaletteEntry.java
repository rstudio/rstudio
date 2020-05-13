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
package org.rstudio.studio.client.application.ui;

import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppCommand.Context;
import org.rstudio.core.client.command.KeySequence;

/**
 * AppCommandPaletteEntry is a widget that represents an AppCommand in RStudio's
 * command palette.
 */
public class AppCommandPaletteEntry extends CommandPaletteEntry
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
      command_.execute();
   }
   
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

   private String label_;
   private final AppCommand command_;
}
