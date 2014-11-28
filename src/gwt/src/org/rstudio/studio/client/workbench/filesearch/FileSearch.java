package org.rstudio.studio.client.workbench.filesearch;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileSearch
{
   @Inject
   public FileSearch(CodeToolsServerOperations server,
                     SessionInfo sessionInfo)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   private CodeToolsServerOperations server_;
   private FileSearchWidget fileSearchWidget_;
   private Session session_;
   private DocDisplay docDisplay_;
   
   @Inject
   void initialize(CodeToolsServerOperations server,
                   Session session)
   {
      server_ = server;
      session_ = session;
   }
   
   public void listAllFiles(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      FileSystemItem projectDir = session_.getSessionInfo().getActiveProjectDir();
      
      // bail if we're not in a project
      if (projectDir == null)
         return;
      
      // list files
      server_.listAllFiles(
            projectDir.getPath(),
            "",
            new ServerRequestCallback<JsArrayString>()
            {
               @Override
               public void onResponseReceived(JsArrayString files)
               {
                  displayFiles(files);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
      
   }
   
   private void displayFiles(JsArrayString files)
   {
      // display 'find file' widget
      if (fileSearchWidget_ == null)
         fileSearchWidget_ = new FileSearchWidget(docDisplay_);
      
      fileSearchWidget_.setFilesAndShow(files);
   }
}
