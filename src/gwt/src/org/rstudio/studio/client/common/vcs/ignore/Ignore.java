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
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.inject.Inject;
import com.google.inject.Provider;

//TODO: ignore on nothing should bring up ignore on root

//TODO: ignore on directory should bring up ignores for THAT directory

//TODO: test ignore on windows (CR/LF)

//TODO: add explanatory text and link at top

//TODO: add directory label at top; OR
//TODO: add directory chooser at top (only way to edit existing)



public class Ignore
{
   public interface Strategy
   {
      public interface Filter
      {
         boolean includeFile(FileSystemItem file);
      }
      
      String getCaption();

      Filter getFilter();
      
      void getIgnores(String path, 
                      ServerRequestCallback<ProcessResult> requestCallback);

      void setIgnores(String path,
                      String ignores,
                      ServerRequestCallback<ProcessResult> requestCallback);
   }
   
   public interface Display
   {
      void showDialog(String caption, String ignores);
    
      ProgressIndicator progressIndicator();
      HasClickHandlers saveButton();
     
      String getIgnored();
   }
   
   @Inject
   public Ignore(GlobalDisplay globalDisplay,
                 Provider<Display> pDisplay)
   {
      globalDisplay_ = globalDisplay;
      pDisplay_ = pDisplay;
   }
   
   public void showDialog(final ArrayList<String> paths,
                          final Strategy strategy)
   {
      // show progress
      final ProgressIndicator globalIndicator = new GlobalProgressDelayer(
                       globalDisplay_, 
                       500,
                       "Getting ignored files for path...").getIndicator();
      
      // derive an ignore list
      final IgnoreList ignoreList = createIgnoreList(paths, 
                                                     strategy.getFilter());
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
            
            if (checkForProcessError(strategy.getCaption(), result))
               return;
                
            // show the ignore dialog
            String ignored = getIgnored(ignoreList, result.getOutput());
            showDialog(ignoreList.getPath(), ignored, strategy);
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
      display.saveButton().addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            indicator.onProgress("Setting ignored files for path...");
            
            strategy.setIgnores(
                  initialPath, 
                  display.getIgnored(), 
                  new ServerRequestCallback<ProcessResult>() {

               @Override
               public void onResponseReceived(ProcessResult result)
               {
                  indicator.onCompleted();
                  checkForProcessError(strategy.getCaption(), result);
               }
                     
               @Override
               public void onError(ServerError error)
               {
                  indicator.onError(error.getUserMessage()); 
               }   
            }); 
            
         }   
      });
      
      display.showDialog(strategy.getCaption(), ignores);
   }
   
   // compute the new list of ignores based on the initial/existing
   // set of paths and path(s) the user wants to add
   private IgnoreList createIgnoreList(ArrayList<String> paths,
                                       Ignore.Strategy.Filter filter)   
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

   
   private class IgnoreList
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
   
   private final GlobalDisplay globalDisplay_;
   private final Provider<Display> pDisplay_;
   
}
