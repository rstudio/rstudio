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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewCompletedEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewOutputEvent;
import org.rstudio.studio.client.htmlpreview.events.HTMLPreviewStartedEvent;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewParams;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
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
      
      void showPreview(String sourceFile,
                       String url, 
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
                               HTMLPreviewServerOperations server)
   {
      view_ = view;
      globalDisplay_ = globalDisplay;
      server_ = server;
      
      binder.bind(commands, this);
      
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
            view_.showProgress("Generating preview...");
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
               view_.closeProgress();
               view_.showPreview(
                  result.getSourceFile(),
                  server_.getApplicationURL(result.getPreviewURL()),
                  result.getEnableScripts());
            }
            else
            {
               view_.setProgressCaption("Preview failed");
               view_.stopProgress();
            }
         }
      });
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
   
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   private final HTMLPreviewServerOperations server_;
}
