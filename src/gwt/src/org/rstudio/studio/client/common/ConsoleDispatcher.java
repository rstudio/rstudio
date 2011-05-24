package org.rstudio.studio.client.common;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.FilenameTransform;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class ConsoleDispatcher
{
   @Inject
   public ConsoleDispatcher(EventBus eventBus,
                            Commands commands,
                            FileDialogs fileDialogs,
                            WorkbenchContext workbenchContext,
                            RemoteFileSystemContext fsContext)
   {
      eventBus_ = eventBus;
      commands_ = commands;
      fileDialogs_ = fileDialogs;
      workbenchContext_ = workbenchContext;
      fsContext_ = fsContext;
   }

   public void executeSetWd(FileSystemItem dir, boolean activateConsole)
   {
      String escaped = dir.getPath().replaceAll("\\\\", "\\\\\\\\");
      if (escaped.equals("~"))
         escaped = "~/";
      eventBus_.fireEvent(
            new SendToConsoleEvent("setwd(\"" + escaped + "\")", true));
      
      if (activateConsole)
         commands_.activateConsole().execute();
   }
   
   public void executeCommand(String command, FileSystemItem targetFile)
   {
      String code = command + "(\"" + targetFile.getPath() + "\")";
      eventBus_.fireEvent(new SendToConsoleEvent(code, true));
   }
   
   
   public void saveFileAsThenExecuteCommand(String caption,
                                            final String defaultExtension,
                                            final String command)
   {
      fileDialogs_.saveFile(
            caption,
            fsContext_,
            workbenchContext_.getCurrentWorkingDir(),
            new FilenameTransform()
            {
               public String transform(String filename)
               {
                  if (defaultExtension != null)
                  {
                     // auto-append .RData if that isn't the extension
                     String ext = FileSystemItem.getExtensionFromPath(filename);
                     return ext.equalsIgnoreCase(defaultExtension)
                              ? filename
                              : filename + defaultExtension;
                  }
                  else
                  {
                     return filename;
                  }
               }
            },
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(
                     FileSystemItem input,
                     ProgressIndicator indicator)
               {
                  if (input == null)
                     return;
                  
                  executeCommand(command, input);
                  indicator.onCompleted();
               }
            });
   }
   
   public void chooseFileThenExecuteCommand(String caption, 
                                            final String command)
   {
      fileDialogs_.openFile(
            caption,
            fsContext_,
            workbenchContext_.getCurrentWorkingDir(),
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(FileSystemItem input, ProgressIndicator indicator)
               {
                  if (input == null)
                     return;
                  
                  executeCommand(command, input);
                  indicator.onCompleted();
               }
            });
      
   }
   
   
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileDialogs fileDialogs_;
   private final WorkbenchContext workbenchContext_;
   private final RemoteFileSystemContext fsContext_;
}
