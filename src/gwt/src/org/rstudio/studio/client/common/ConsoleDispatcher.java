/*
 * ConsoleDispatcher.java
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
package org.rstudio.studio.client.common;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
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
                            Session session,
                            RemoteFileSystemContext fsContext)
   {
      eventBus_ = eventBus;
      commands_ = commands;
      fileDialogs_ = fileDialogs;
      workbenchContext_ = workbenchContext;
      session_ = session;
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
      executeCommand(command, targetFile.getPath());
   }
   
   public void executeCommand(String command, String argument)
   {
      String code = command + "(\"" + argument + "\")";
      eventBus_.fireEvent(new SendToConsoleEvent(code, true));
   }
   
   public void executeCommandWithFileEncoding(String command,
                                              String path,
                                              String encoding,
                                              boolean contentIsAscii)
   {
      String escapedPath = escapedPath(path);
      String systemEncoding = session_.getSessionInfo().getSystemEncoding();
      boolean isSystemEncoding =
       normalizeEncoding(encoding).equals(normalizeEncoding(systemEncoding));
      
      StringBuilder code = new StringBuilder();
      code.append(command + "(" + escapedPath);
      
      if (!isSystemEncoding && !contentIsAscii)
      {
         code.append(", encoding = '" +
               (!StringUtil.isNullOrEmpty(encoding) ? encoding : "UTF-8") +
               "'");
      }
      code.append(")");
      eventBus_.fireEvent(new SendToConsoleEvent(code.toString(), true));
   }
   
   
   public void saveFileAsThenExecuteCommand(String caption,
                                            final String defaultExtension,
                                            boolean forceExtension,
                                            final String command)
   {
      fileDialogs_.saveFile(
            caption,
            fsContext_,
            workbenchContext_.getCurrentWorkingDir(),
            defaultExtension,
            forceExtension,
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
  
   
   public void executeSourceCommand(String path,
                                    TextFileType fileType,
                                    String encoding, 
                                    boolean contentKnownToBeAscii,
                                    boolean echo,
                                    boolean focus,
                                    boolean debug)
   {
       
      StringBuilder code = new StringBuilder();
      
      if (fileType.isCpp())
      {
         // use a relative path if possible
         String relativePath = FileSystemItem.createFile(path).getPathRelativeTo(
                                       workbenchContext_.getCurrentWorkingDir());
         if (relativePath != null)
            path = relativePath;
       
         code.append("Rcpp::sourceCpp(" + escapedPath(path) + ")");
      }
      else
      {
         String escapedPath = escapedPath(path);
         String systemEncoding = session_.getSessionInfo().getSystemEncoding();
         boolean isSystemEncoding =
          normalizeEncoding(encoding) == normalizeEncoding(systemEncoding);
         
         if (contentKnownToBeAscii || isSystemEncoding)
            code.append((debug ? "debugSource" : "source") + 
                     "(" + escapedPath);
         else
         {
            code.append((debug ? "debugSource" : "source") + 
                  "(" + escapedPath + ", encoding = '" +
                  (!StringUtil.isNullOrEmpty(encoding) ? encoding : "UTF-8") +
                  "'");
         }
         
         if (echo)
            code.append(", echo=TRUE");
         code.append(")");
      }
      
      
      eventBus_.fireEvent(new SendToConsoleEvent(code.toString(), true));
      
      if (focus)
         commands_.activateConsole().execute();
   }
   
   public static String escapedPath(String path)
   {
      String escapedPath = "'" +
                           path.replace("\\", "\\\\").replace("'", "\\'") +
                           "'";
      return escapedPath;
   }
   
   private String normalizeEncoding(String str)
   {
      return StringUtil.notNull(str).replaceAll("[- ]", "").toLowerCase();
   }
   
   
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileDialogs fileDialogs_;
   private final WorkbenchContext workbenchContext_;
   private final Session session_;
   private final RemoteFileSystemContext fsContext_;
}
