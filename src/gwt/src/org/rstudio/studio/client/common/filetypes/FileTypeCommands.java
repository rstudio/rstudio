/*
 * FileTypeCommands.java
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

import java.util.ArrayList;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileTypeCommands
{
   public static class CommandWithId
   {
      private CommandWithId(String commandId, AppCommand command)
      {
         this.commandId = commandId;
         this.command = command;
      }

      public final String commandId;
      public final AppCommand command;
   }

   @Inject
   public FileTypeCommands(EventBus eventBus, Commands commands)
   {
      eventBus_ = eventBus;
      commands_ = commands;

      addType(FileTypeRegistry.TEXT, "_Text File");
      addType(FileTypeRegistry.MARKDOWN, "_Markdown File");
      addType(FileTypeRegistry.TEX, "Te_X Document");
      addType(FileTypeRegistry.RD, "R _Documentation");
   }

   
   public ArrayList<CommandWithId> commandsWithIds()
   {
      return fileTypeCommands_;
   }
   
   public TextFileType[] statusBarFileTypes()
   {
      return new TextFileType[] {FileTypeRegistry.R,
                                 FileTypeRegistry.SWEAVE,
                                 FileTypeRegistry.TEXT,
                                 FileTypeRegistry.HTML,
                                 FileTypeRegistry.MARKDOWN,
                                 FileTypeRegistry.TEX,
                                 FileTypeRegistry.RD};
   }
   
   
   private void addType(final TextFileType fileType, String menuLabel)
   {
      AppCommand command = new AppCommand();
      command.setMenuLabel(menuLabel);
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
   
   private static String commandIdForType(FileType fileType)
   {
      return "filetype_" + fileType.getTypeId();
   }

   private final EventBus eventBus_;
   private final Commands commands_;
   private ArrayList<CommandWithId> fileTypeCommands_ =
         new ArrayList<CommandWithId>();

}
