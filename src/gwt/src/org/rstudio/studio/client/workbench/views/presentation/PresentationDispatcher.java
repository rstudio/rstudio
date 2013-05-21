/*
 * PresentationDispatcher.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationCommand;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.inject.Inject;

public class PresentationDispatcher 
{   
   @Inject
   public PresentationDispatcher(EventBus eventBus,
                                 FileTypeRegistry fileTypeRegistry)
   {
      eventBus_ = eventBus;
      fileTypeRegistry_ = fileTypeRegistry;
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
      else
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
   }
   
   protected void fireOpenSourceFileEvent(OpenPresentationSourceFileEvent event) 
   {
      eventBus_.fireEvent(event);
   }
       
   private String getPresentationPath(String file)
   {
      FileSystemItem presentationFile = FileSystemItem.createFile(
                                         context_.getPresentationFilePath());
      return presentationFile.getParentPath().completePath(file);
   }
     
   protected Context context_;
   protected final EventBus eventBus_;
   protected final FileTypeRegistry fileTypeRegistry_;
}