/*
 * PresentationDispatcher.java
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

package org.rstudio.studio.client.workbench.views.presentation;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileEvent;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationCommand;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;

public class PresentationDispatcher 
{   
   @Inject
   public PresentationDispatcher(PresentationServerOperations server,
                                 GlobalDisplay globalDisplay,
                                 EventBus eventBus,
                                 Commands commands,
                                 FileTypeRegistry fileTypeRegistry,
                                 Session session,
                                 WorkbenchContext workbenchContext)
   {
      eventBus_ = eventBus;
      fileTypeRegistry_ = fileTypeRegistry;
      server_ = server;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      session_ = session;
      workbenchContext_ = workbenchContext;
   }
   
   public interface Context
   {
      void pauseMedia();
      String getPresentationFilePath();
   }

   
   public void setContext(Context context)
   {
      context_ = context;
   }
  
   public void dispatchCommand(JavaScriptObject jsCommand)
   {
      // cast
      PresentationCommand command = jsCommand.cast();
      
      // crack parameters
      String param1 = null, param2 = null;
      String params = command.getParams();
      if (params.length() > 0)
      {
         // find the first space and split on that
         int spaceLoc = params.indexOf(' ');
         if (spaceLoc == -1)
         {
            param1 = params;
         }
         else
         {
            param1 = params.substring(0, spaceLoc);
            param2 = params.substring(spaceLoc+1);
         } 
      }
      
      String cmdName = command.getName().toLowerCase();
      
      if (cmdName.equals("source"))
         performSourceCommand(param1, param2);
      else if (session_.getSessionInfo().getPresentationCommands()) 
         performOtherCommand(cmdName, params, param1, param2);
   }
   
   private void performSourceCommand(String param1, String param2)
   {   
      if (param1 != null)
      {
         // get filename and type
         FileSystemItem file = FileSystemItem.createFile(
                                                  getPresentationPath(param1));
         TextFileType fileType = fileTypeRegistry_.getTextTypeForFile(file); 
         
         // check for a file position and/or pattern
         FilePosition pos = null;
         String pattern = null;
         if (param2 != null)
         {
            if (param2.length() > 2 && 
                param2.startsWith("/") && param2.endsWith("/"))
            {
               pattern = param2.substring(1, param2.length()-1);
            }
            else
            {
               int line = StringUtil.parseInt(param2, 0);
               if (line > 0)
                  pos = FilePosition.create(line, 1);
            }
         }
         
         // dispatch
         fireOpenSourceFileEvent(new OpenPresentationSourceFileEvent(
                                                             file, 
                                                             fileType,
                                                             pos,
                                                             pattern));
      }  
   }
   
   protected void performOtherCommand(String cmdName, 
         String params, 
         String param1, 
         String param2)
   {
      if (cmdName == "help-doc")
         performHelpDocCommand(param1, param2);
      else if (cmdName == "help-topic")
         performHelpTopicCommand(param1, param2);
      else if (cmdName == "console")
         performConsoleCommand(params);
      else if (cmdName == "console-input")
         performConsoleInputCommand(params);
      else if (cmdName == "execute")
         performExecuteCommand(params);
      else if (cmdName == "pause")
         performPauseCommand();
      else 
      {
         globalDisplay_.showErrorMessage(
               "Unknown Presentation Command", 
               cmdName + ": " + params);
      }
   }

   protected void fireOpenSourceFileEvent(OpenPresentationSourceFileEvent event) 
   {
      eventBus_.fireEvent(event);
   }


   private void performHelpDocCommand(String param1, String param2)
   {
      if (param1 != null)
      {
         String docFile = getPresentationPath(param1);
         String url = "help/presentation/?file=" + URL.encodeQueryString(docFile);
         eventBus_.fireEvent(new ShowHelpEvent(url));  
      }
   }

   private void performHelpTopicCommand(String param1, String param2)
   {
      // split on :: if it's there
      if (param1 != null)
      {
         String topic = param1;
         String packageName = null;
         int delimLoc = param1.indexOf("::");
         if (delimLoc != -1)
         {
            packageName = param1.substring(0, delimLoc);
            topic = param1.substring(delimLoc+2);
         }

         server_.showHelpTopic(topic, packageName, RCompletionType.FUNCTION);
      }
   }

   private void performConsoleCommand(String params)
   {  
      String[] cmds = params.split(",");
      for (String cmd : cmds)
      {         
         cmd = cmd.trim();
         if (cmd.equals("maximize"))
            commands_.maximizeConsole().execute();
         else if (cmd.equals("clear"))
            commands_.consoleClear().execute();
         else
            globalDisplay_.showErrorMessage("Unknown Console Directive", cmd);
      }
   }

   private void performPauseCommand()
   {
      context_.pauseMedia();
   }

   private void performConsoleInputCommand(String params)
   {
      eventBus_.fireEvent(new SendToConsoleEvent(params, true, false, true));
   }

   private void performExecuteCommand(String params)
   {
      server_.presentationExecuteCode(params, new VoidServerRequestCallback());
   }

   protected String getPresentationPath(String file)
   {
      FileSystemItem presentationFile = FileSystemItem.createFile(
                                         context_.getPresentationFilePath());
      return presentationFile.getParentPath().completePath(file);
   }
     
   protected Context context_;
   private final EventBus eventBus_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final PresentationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final Session session_;
   @SuppressWarnings("unused")
   private final WorkbenchContext workbenchContext_;
   
}
