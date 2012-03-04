/*
 * PDFViewerPresenter.java
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
package org.rstudio.studio.client.pdfviewer;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.compilepdf.dialog.CompilePdfProgressDialog;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfStartedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.pdfviewer.events.InitCompleteEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PDFViewerPresenter implements IsWidget, 
                                           CompilePdfStartedEvent.Handler,
                                           CompilePdfCompletedEvent.Handler
{
   public interface Display extends IsWidget
   {     
      void setURL(String url);
      HandlerRegistration addInitCompleteHandler(
                                             InitCompleteEvent.Handler handler);
      void closeWindow();
   }
   
   @Inject
   public PDFViewerPresenter(Display view,
                             EventBus eventBus,
                             FileTypeRegistry fileTypeRegistry,
                             CompilePdfServerOperations server,
                             GlobalDisplay globalDisplay)
   {
      view_ = view;
      fileTypeRegistry_ = fileTypeRegistry;
      server_ = server;
      globalDisplay_ = globalDisplay;
      
      eventBus.addHandler(CompilePdfStartedEvent.TYPE, this);
      eventBus.addHandler(CompilePdfCompletedEvent.TYPE, this);
      
      Window.addWindowClosingHandler(new ClosingHandler() {

         @Override
         public void onWindowClosing(ClosingEvent event)
         {
            if (compileIsRunning_)
               terminateRunningCompile();
         }
         
      });
   }
   
   @Override
   public void onCompilePdfStarted(CompilePdfStartedEvent event)
   {
      compileIsRunning_ = true;
      
      dismissProgressDialog();
      
      activeProgressDialog_  = new CompilePdfProgressDialog();
      
      activeProgressDialog_.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            if (compileIsRunning_)
            {
               terminateRunningCompile();
            }
            else
            {
               dismissProgressDialog();
            }
         }

      });

      activeProgressDialog_.addSelectionCommitHandler(
            new SelectionCommitHandler<CodeNavigationTarget>() {

               @Override
               public void onSelectionCommit(
                     SelectionCommitEvent<CodeNavigationTarget> event)
               {
                  CodeNavigationTarget target = event.getSelectedItem();
                  
                  editFile(FileSystemItem.createFile(target.getFile()), 
                                                     target.getPosition());
                  dismissProgressDialog();
               }
            });

      activeProgressDialog_.showModal();
   }
   
   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      compileIsRunning_ = false;
      
      if (event.getSucceeded())
         view_.setURL(event.getPdfUrl());
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   public HandlerRegistration addInitCompleteHandler(InitCompleteEvent.Handler handler)
   {
      return view_.addInitCompleteHandler(handler);
   }

   private void editFile(FileSystemItem file, FilePosition position)
   {
      fileTypeRegistry_.editFile(file, position);
 
      // firefox and chrome frame won't allow window re-activation
      // so we close the parent window to force this
      if (BrowseCap.isFirefox() || BrowseCap.isChromeFrame())
         view_.closeWindow();
   }
   
   private void dismissProgressDialog()
   {
      if (activeProgressDialog_ != null)
      {
         activeProgressDialog_.dismiss();
         activeProgressDialog_ = null;
      }
   }
   
   private void terminateRunningCompile()
   {
      server_.terminateCompilePdf(new ServerRequestCallback<Boolean>() 
      {
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error", 
                                            error.getUserMessage());
            
         }
      });
   }

   private boolean compileIsRunning_ = false;
   private CompilePdfProgressDialog activeProgressDialog_;
   private final Display view_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final CompilePdfServerOperations server_;
   private GlobalDisplay globalDisplay_;
}
