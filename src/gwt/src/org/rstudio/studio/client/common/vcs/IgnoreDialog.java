/*
 * IgnoreDialog.java
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
package org.rstudio.studio.client.common.vcs;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.vcs.SVNServerOperations.ProcessResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.edit.ui.EditDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

// TODO: test ignore on windows (CR/LF)

// TODO: implement ignore for git

// TODO: add explanatory text and link at top

// TODO: add directory label at top; OR
// TODO: add directory chooser at top (only way to edit existing)


public class IgnoreDialog 
{    
   public static void show(final ArrayList<String> paths,
                           final IgnoreStrategy strategy)
   {
      // show progress
      final ProgressIndicator globalIndicator = new GlobalProgressDelayer(
                       RStudioGinjector.INSTANCE.getGlobalDisplay(), 
                       500,
                       "Getting ignored files for path...").getIndicator();
      
      // derive an ignore list
      final IgnoreList ignoreList = createIgnoreList(paths);
      if (ignoreList == null)
      {
         globalIndicator.onCompleted();
         return;
      }
        
      // get existing ignores
      strategy.getIgnores(ignoreList.getPath(), 
                          new ServerRequestCallback<ProcessResult>() {
 
         @Override
         public void onResponseReceived(final ProcessResult result)
         {
            globalIndicator.onCompleted();
            
            if (checkForProcessError(result))
               return;
                         
               EditDialog dlg = new EditDialog(
                             getIgnored(ignoreList, result.getOutput()), 
                             false,
                             false,
                             new ProgressOperationWithInput<String>() {

               @Override
               public void execute(String ignores, 
                                   final ProgressIndicator indicator)
               {
                  if (ignores == null)
                  {
                     indicator.onCompleted();
                     return;
                  }
                  
                  indicator.onProgress("Setting ignored files for path...");
                  
                  strategy.setIgnores(
                        ignoreList.getPath(), 
                        ignores, 
                        new ServerRequestCallback<ProcessResult>() {

                     @Override
                     public void onResponseReceived(ProcessResult result)
                     {
                        indicator.onCompleted();
                        checkForProcessError(result);
                     }
                           
                     @Override
                     public void onError(ServerError error)
                     {
                        indicator.onError(error.getUserMessage()); 
                     }   
                  }); 
               }  
            });
            
            dlg.setText(strategy.getCaption());
            dlg.showModal();
         }
         
         @Override
         public void onError(ServerError error)
         {
            globalIndicator.onError(error.getUserMessage());
         }
         
         // check for an error and show the console progress dialog
         // if there was one
         private boolean checkForProcessError(ProcessResult result)
         {
            if (result.getExitCode() != 0)
            {
               new ConsoleProgressDialog(strategy.getCaption(),
                                         result.getOutput(),
                                         result.getExitCode()).showModal();
               return true;
            }
            else
            {
               return false;
            }
         }
      });
      
   }
   
   // compute the new list of ignores based on the initial/existing
   // set of paths and path(s) the user wants to add
   private static IgnoreList createIgnoreList(ArrayList<String> paths)   
   {
      if (paths.size() == 0)
         return null;
      
      // get the parent path of the first element
      FileSystemItem firstPath = FileSystemItem.createFile(paths.get(0));
      String parentPath = firstPath.getParentPathString();
      
      // confirm that all of the elements start with that path and take the
      // remainder of their paths for our list
      ArrayList<String> ignored = new ArrayList<String>();
      for (String path : paths)
      {
         String thisParent = 
                      FileSystemItem.createFile(path).getParentPathString();
         
         if (!parentPath.equals(thisParent))
         {
            GlobalDisplay gDisp = RStudioGinjector.INSTANCE.getGlobalDisplay();
            gDisp.showMessage(
                  MessageDialog.ERROR, 
                  "Error: Multiple Directories",
                  "The selected files are not all within the same directory " +
                  "(you can only ignore multiple files in one operation if " +
                  "they are located within the same directory).");
                
            return null;
         }
         
         // compute the parent relative directory
         if (parentPath.length() == 0)
            ignored.add(path);
         else
            ignored.add(path.substring(thisParent.length() + 1));
      }
      
      return new IgnoreList(parentPath, ignored);
   }
   
   private static String getIgnored(IgnoreList ignoreList,
                                    String existingIgnored)
   {
      // split existing ignored into list
      ArrayList<String> ignored = new ArrayList<String>();
      Iterable<String> existing = StringUtil.getLineIterator(existingIgnored);
      for (String item : existing)
      {
         item = item.trim();
         if (item.length() > 0)
            ignored.add(item);
      }
 

      // for any element not already in the list add it
      for (String item : ignoreList.getFiles())
         if (!ignored.contains(item))
            ignored.add(item);
            
      // return as a string
      return StringUtil.join(ignored, "\n");
   }

   
   
   private static class IgnoreList
   {
      public IgnoreList(String path, ArrayList<String> files)
      {
         path_ = path;
         files_ = files;
      }
      
      public String getPath() { return path_; }
      public ArrayList<String> getFiles() { return files_; }
      
      private final String path_;
      private final ArrayList<String> files_;
   }
}
