/*
 * Ignore.java
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
package org.rstudio.studio.client.common.vcs.ignore;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.vcs.ProcessResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class Ignore
{
   public interface Strategy
   {
      public interface Filter
      {
         boolean includeFile(FileSystemItem file);
      }
      
      String getDialogCaption();
      
      String getIgnoresCaption();

      String getHelpLinkName();
      
      Filter getFilter();
      
      void getIgnores(String path, 
                      ServerRequestCallback<ProcessResult> requestCallback);

      void setIgnores(String path,
                      String ignores,
                      ServerRequestCallback<ProcessResult> requestCallback);
   }
   
   public interface Display
   {
      void setDialogCaption(String caption);
      void setIgnoresCaption(String caption);
      void setHelpLinkName(String helpLinkName);
      ProgressIndicator progressIndicator();
      HasClickHandlers saveButton();
      
      void setCurrentPath(String path);
      String getCurrentPath();
      HandlerRegistration addPathChangedHandler(
                                    ValueChangeHandler<String> handler);
      
      void setIgnored(String ignored);
      String getIgnored();
      
      void focusIgnored();
      
      void scrollToBottom();
      
      void showModal();
   }
   
   @Inject
   public Ignore(GlobalDisplay globalDisplay,
                 Session session,
                 Provider<Display> pDisplay)
   {
      globalDisplay_ = globalDisplay;
      session_ = session;
      pDisplay_ = pDisplay;
   }
   
   public void showDialog(ArrayList<String> paths, Strategy strategy)
   {
      IgnoreList ignoreList = createIgnoreList(paths, strategy.getFilter());
      if (ignoreList != null)
         showDialog(ignoreList, strategy);
   }
   
   public void showDialog(final IgnoreList ignoreList,
                          final Strategy strategy)
   {
      // show progress
      final ProgressIndicator globalIndicator = new GlobalProgressDelayer(
                       globalDisplay_, 
                       500,
                       "Getting ignored files for path...").getIndicator();
      
      // get existing ignores
      final String fullPath = projPathToFullPath(ignoreList.getPath());
      strategy.getIgnores(fullPath, 
                          new ServerRequestCallback<ProcessResult>() {
 
         @Override
         public void onResponseReceived(final ProcessResult result)
         {
            globalIndicator.onCompleted();
            
            if (checkForProcessError(strategy.getDialogCaption(), result))
               return;
                
            // show the ignore dialog
            String ignored = getIgnored(ignoreList, result.getOutput());
            showDialog(fullPath, ignored, strategy);
         }
         
         @Override
         public void onError(ServerError error)
         {
            globalIndicator.onError(error.getUserMessage());
         }
      });
   }
   
   private void showDialog(final String initialPath,
                           String ignores,
                           final Strategy strategy)
   {
      final Display display = pDisplay_.get();
      final ProgressIndicator indicator = display.progressIndicator();
      
      display.setDialogCaption(strategy.getDialogCaption());
      display.setIgnoresCaption(strategy.getIgnoresCaption());
      display.setHelpLinkName(strategy.getHelpLinkName());
      display.setCurrentPath(initialPath);
      display.setIgnored(ignores);
      display.scrollToBottom();
      
      display.addPathChangedHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {  
            display.setIgnored("");
            
            indicator.onProgress("Getting ignored files for path...");
            
            strategy.getIgnores(display.getCurrentPath(),
                  new ServerRequestCallback<ProcessResult>() {

               @Override
               public void onResponseReceived(final ProcessResult result)
               {
                  indicator.clearProgress();

                  if (checkForProcessError(strategy.getDialogCaption(), result))
                     return;

                  display.setIgnored(result.getOutput());
                  display.focusIgnored();
               }

               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getUserMessage());
               }});
            }
      });
      
      display.saveButton().addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            indicator.onProgress("Setting ignored files for path...");
            
            strategy.setIgnores(
                  display.getCurrentPath(), 
                  display.getIgnored(), 
                  new ServerRequestCallback<ProcessResult>() {

               @Override
               public void onResponseReceived(ProcessResult result)
               {
                  indicator.onCompleted();
                  checkForProcessError(strategy.getDialogCaption(), result);
               }
                     
               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getUserMessage()); 
               }   
            }); 
            
         }   
      });
      
      display.showModal();
   }
   
   private String projPathToFullPath(String projPath)
   {
      FileSystemItem projDir = session_.getSessionInfo().getActiveProjectDir();
      return projPath.length() > 0 ? projDir.completePath(projPath) :
                                     projDir.getPath();
   }
   
   // compute the new list of ignores based on the initial/existing
   // set of paths and path(s) the user wants to add
   private IgnoreList createIgnoreList(ArrayList<String> paths,
                                       Ignore.Strategy.Filter filter)   
   {
      // special case for empty path list -- make the project root the 
      // parent path and return an empty list
      if (paths.size() == 0)
         return new IgnoreList("", new ArrayList<String>());
       
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
         
         if (parentPath != thisParent)
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
         
         // apply a filter if we have one
         if (filter != null)
         {
            FileSystemItem file = FileSystemItem.createFile(path);
            if (!filter.includeFile(file))
               continue;
         }
         
         // compute the parent relative directory
         if (parentPath.length() == 0)
            ignored.add(path);
         else
            ignored.add(path.substring(thisParent.length() + 1));
      }
            
      return new IgnoreList(parentPath, ignored);
   }
   
   private String getIgnored(IgnoreList ignoreList, String existingIgnored)
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
   
   // check for an error and show the console progress dialog
   // if there was one
   private boolean checkForProcessError(String caption, ProcessResult result)
   {
      if (result.getExitCode() != 0)
      {
         new ConsoleProgressDialog(caption,
                                   result.getOutput(),
                                   result.getExitCode()).showModal();
         return true;
      }
      else
      {
         return false;
      }
   }
  
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
   private final Provider<Display> pDisplay_;
   
}
