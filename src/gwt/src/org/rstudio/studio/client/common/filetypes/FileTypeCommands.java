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

      addType(FileTypeRegistry.R);
      addType(FileTypeRegistry.TEXT);
      addType(FileTypeRegistry.SWEAVE);
      addType(FileTypeRegistry.TEX);
   }

   
   public ArrayList<CommandWithId> commandsWithIds()
   {
      return fileTypeCommands_;
   }
   
   public ArrayList<CommandWithId> commandsWithIds(FileType excludeType)
   {
      String excludedTypeId = commandIdForType(excludeType);
      ArrayList<CommandWithId> commands = new ArrayList<CommandWithId>();
      for (CommandWithId cmd : fileTypeCommands_)
         if (!cmd.commandId.equals(excludedTypeId))
            commands.add(cmd);
      return commands;
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
   
   private static String commandIdForType(FileType fileType)
   {
      return "filetype_" + fileType.getTypeId();
   }

   private final EventBus eventBus_;
   private final Commands commands_;
   private ArrayList<CommandWithId> fileTypeCommands_ =
         new ArrayList<CommandWithId>();

}
