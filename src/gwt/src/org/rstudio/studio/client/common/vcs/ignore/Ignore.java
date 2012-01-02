package org.rstudio.studio.client.common.vcs.ignore;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.vcs.ProcessResult;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.inject.Inject;

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
      
   }
   
   @Inject
   public Ignore()
   {
      
   }
   
}
