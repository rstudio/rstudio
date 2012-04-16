/*
 * HTMLPreviewPresenter.java
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
package org.rstudio.studio.client.htmlpreview;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewCompletedEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewOutputEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewStartedEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HTMLPreviewPresenter implements IsWidget
{
   public interface Binder extends CommandBinder<Commands, HTMLPreviewPresenter>
   {}

   
   public interface Display extends IsWidget
   {
      void showProgress(String caption);
      void setProgressCaption(String caption);
      void showProgressOutput(String output);
      void stopProgress();
      void closeProgress();
      HandlerRegistration addProgressClickHandler(ClickHandler handler);
      
      void showPreview(String url,
                       String htmlFile, 
                       boolean enableSaveAs,
                       boolean enableScripts);
      
      void print();
   }
   
   @Inject
   public HTMLPreviewPresenter(Display view,
                               Binder binder,
                               Commands commands,
                               GlobalDisplay globalDisplay,
                               EventBus eventBus,
                               Satellite satellite,
                               Session session,
                               FileDialogs fileDialogs,
                               RemoteFileSystemContext fileSystemContext,
                               HTMLPreviewServerOperations server)
   {
      view_ = view;
      globalDisplay_ = globalDisplay;
      server_ = server;
      session_ = session;
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
      
      binder.bind(commands, this);
      
      // disable print in desktop mode (qt printer can't handle page-breaks)
      if (Desktop.isDesktop())
         commands.printHtmlPreview().remove();
      
      satellite.addCloseHandler(new CloseHandler<Satellite>() {
         @Override
         public void onClose(CloseEvent<Satellite> event)
         {
            if (previewRunning_)
               terminateRunningPreview();
         }
      });
      
      eventBus.addHandler(HTMLPreviewStartedEvent.TYPE, 
                          new HTMLPreviewStartedEvent.Handler()
      {
         @Override
         public void onHTMLPreviewStarted(HTMLPreviewStartedEvent event)
         {
            previewRunning_ = true;
            view_.showProgress("Knitting...");
            view_.addProgressClickHandler(new ClickHandler() {
               @Override
               public void onClick(ClickEvent event)
               {
                  if (previewRunning_)
                     terminateRunningPreview();
                  else
                     view_.closeProgress();
               }
               
            });
         }
      });
      
      eventBus.addHandler(HTMLPreviewOutputEvent.TYPE, 
                          new HTMLPreviewOutputEvent.Handler()
      {
         @Override
         public void onHTMLPreviewOutput(HTMLPreviewOutputEvent event)
         {
            view_.showProgressOutput(event.getOutput());
         }
      });
      
      eventBus.addHandler(HTMLPreviewCompletedEvent.TYPE, 
                          new HTMLPreviewCompletedEvent.Handler()
      { 
         @Override
         public void onHTMLPreviewCompleted(HTMLPreviewCompletedEvent event)
         {
            previewRunning_ = false;
            
            HTMLPreviewResult result = event.getResult();
            if (result.getSucceeded())
            {
               lastSuccessfulPreview_ = result;
               view_.closeProgress();
               view_.showPreview(
                  server_.getApplicationURL(result.getPreviewURL()),
                  result.getHtmlFile(),
                  result.getEnableSaveAs(),
                  result.getEnableScripts());
            }
            else
            {
               view_.setProgressCaption("Preview failed");
               view_.stopProgress();
            }
         }
      });
      
      new StringStateValue(
            MODULE_HTML_PREVIEW,
            KEY_SAVEAS_DIR,
            ClientState.PERSISTENT,
            session_.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(String value)
         {
            savePreviewDir_ = value;
         }

         @Override
         protected String getValue()
         {
            return savePreviewDir_;
         }
      };
   }
   
   
   public void onActivated(HTMLPreviewParams params)
   {
   }
  
   
   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   @Handler
   public void onOpenHtmlExternal()
   {
      if (lastSuccessfulPreview_ != null)
      {
         String htmlFile = lastSuccessfulPreview_.getHtmlFile();
         if (Desktop.isDesktop())
         {
            Desktop.getFrame().showFile(htmlFile);
         }
         else
         {
            globalDisplay_.openWindow(server_.getFileUrl(
                                       FileSystemItem.createFile(htmlFile)));
         }
      }
   }
   
   @Handler
   public void onSaveHtmlPreviewAs()
   {
      if (lastSuccessfulPreview_ != null)
      {
         FileSystemItem defaultDir = savePreviewDir_ != null ?
               FileSystemItem.createDir(savePreviewDir_) :
               FileSystemItem.home();
         
         final FileSystemItem sourceFile = FileSystemItem.createFile(
                                          lastSuccessfulPreview_.getHtmlFile());
         
         FileSystemItem initialFilePath = 
            FileSystemItem.createFile(defaultDir.completePath(
                                                         sourceFile.getStem()));
         
         fileDialogs_.saveFile(
            "Save File As", 
             fileSystemContext_, 
             initialFilePath, 
             sourceFile.getExtension(),
             false, 
             new ProgressOperationWithInput<FileSystemItem>(){

               @Override
               public void execute(FileSystemItem targetFile,
                                   ProgressIndicator indicator)
               {
                  if (targetFile == null || sourceFile.equalTo(targetFile))
                  {
                     indicator.onCompleted();
                     return;
                  }
                  
                  indicator.onProgress("Saving File...");
      
                  server_.copyFile(sourceFile, 
                                   targetFile, 
                                   true,
                                   new VoidServerRequestCallback(indicator));
                  
                  savePreviewDir_ = targetFile.getParentPathString();
                  session_.persistClientState();
               }
         });
      }
   }
   
   @Handler
   public void onPrintHtmlPreview()
   {
      view_.print();
   }

   private void terminateRunningPreview()
   {
      server_.terminatePreviewHTML(new VoidServerRequestCallback());
   }
   
   private final Display view_;
   private boolean previewRunning_ = false;
   private HTMLPreviewResult lastSuccessfulPreview_;
   
   private String savePreviewDir_;
   private static final String MODULE_HTML_PREVIEW = "html_preview";
   private static final String KEY_SAVEAS_DIR = "saveAsDir";
   
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;
   private final Session session_;
   private final RemoteFileSystemContext fileSystemContext_;
   private final HTMLPreviewServerOperations server_;
}
