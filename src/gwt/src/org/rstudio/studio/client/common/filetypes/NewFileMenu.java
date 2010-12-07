/*
 * NewFileMenu.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.inject.Inject;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.command.MenuCallback;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.workbench.commands.Commands;

import java.util.ArrayList;

public class NewFileMenu
{
   private static class CommandWithId
   {
      private CommandWithId(String commandId, AppCommand command)
      {
         this.commandId = commandId;
         this.command = command;
      }

      public final String commandId;
      public final AppCommand command;
   }

   public NewFileMenu()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   public void execute(MenuCallback callback)
   {
      for (CommandWithId cmd : fileTypeCommands_)
         callback.addCommand(cmd.commandId, cmd.command);
   }

   private void addType(final TextFileType fileType)
   {
      AppCommand command = new AppCommand();
      command.setMenuLabel(fileType.getLabel());
      command.setImageResource(fileType.getDefaultIcon());
      command.addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            eventBus_.fireEvent(new OpenSourceFileEvent(null,
                                                        fileType));
         }
      });

      String commandId = commandIdForType(fileType);
      commands_.addCommand(commandId, command);
      fileTypeCommands_.add(new CommandWithId(commandId, command));
   }

   public static String commandIdForType(FileType fileType)
   {
      return "filetype_" + fileType.getTypeId();
   }

   @Inject
   void initialize(EventBus eventBus,
                   Commands commands)
   {
      eventBus_ = eventBus;
      commands_ = commands;

      addType(FileTypeRegistry.TEXT);
      addType(FileTypeRegistry.SWEAVE);
      addType(FileTypeRegistry.TEX);
   }

   private EventBus eventBus_;
   private Commands commands_;
   private ArrayList<CommandWithId> fileTypeCommands_ =
         new ArrayList<CommandWithId>();

}
