/*
 * CompilePdfOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfEvent;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfErrorsEvent;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfOutputEvent;
import org.rstudio.studio.client.workbench.views.output.compilepdf.model.CompilePdfError;
import org.rstudio.studio.client.workbench.views.output.compilepdf.model.CompilePdfServerOperations;


public class CompilePdfOutputPresenter extends BasePresenter
   implements CompilePdfEvent.Handler,
              CompilePdfOutputEvent.Handler, 
              CompilePdfErrorsEvent.Handler
{
   public interface Display extends WorkbenchView, HasEnsureHiddenHandlers
   {
      void clearOutput();
      void showOutput(String output);
      void showErrors(JsArray<CompilePdfError> errors);
   }

   @Inject
   public CompilePdfOutputPresenter(Display view,
                                    GlobalDisplay globalDisplay,
                                    CompilePdfServerOperations server)
   {
      super(view);
      view_ = view;
      globalDisplay_ = globalDisplay;
      server_ = server;
   }
   
   public void confirmClose(final Command onConfirmed)
   {  
      server_.compilePdfRunning(
                  new RequestCallback<Boolean>("Closing Compile PDF...") {
         @Override
         public void onSuccess(Boolean isRunning)
         {  
            if (isRunning)
            {
               confirmTerminateRunningCompile("close the Compile PDF tab", 
                                              onConfirmed);
            }
         }
      });
      
   }

   @Override
   public void onCompilePdf(CompilePdfEvent event)
   {
      view_.bringToFront();
      
      compilePdf(event.getTargetFile(), event.getCompletedAction());
   }
   
   @Override
   public void onCompilePdfOutput(CompilePdfOutputEvent event)
   {
      view_.showOutput(event.getOutput());
   }
   
   @Override
   public void onCompilePdfErrors(CompilePdfErrorsEvent event)
   {
      view_.showErrors(event.getErrors());
   }
   
   
   private void compilePdf(final FileSystemItem targetFile,
                           final String completedAction)
   {
      server_.compilePdf(
            targetFile, 
            completedAction, 
            new RequestCallback<Boolean>("Compiling PDF...") 
            {
               @Override
               protected void onSuccess(Boolean started)
               {
                  if (started)
                  {
                     view_.clearOutput();
                  }
                  else
                  {
                     confirmTerminateRunningCompile(
                           "start a new compilation",
                           new Command() {
   
                              @Override
                              public void execute()
                              {
                                 compilePdf(targetFile, completedAction);
                              }     
                           });
                  }
               }
         });
   }
   
   private void confirmTerminateRunningCompile(String operation,
                                               final Command onTerminated)
   {
      globalDisplay_.showYesNoMessage(
         MessageDialog.WARNING,
         "Stop Running Compile", 
         "There is a PDF compilation currently running. If you " +
         operation + " it will be terminated. Are you " +
         "sure you want to stop the running PDF compilation?", 
         new Operation() {
            @Override
            public void execute()
            {
               server_.terminateCompilePdf(new RequestCallback<Boolean>(
                                       "Terminating PDF compilation...") {
                  @Override
                  protected void onSuccess(Boolean wasTerminated)
                  {
                     if (wasTerminated)
                        onTerminated.execute();           
                  }
               });
            }},
            false);
   }
   
   
   private abstract class RequestCallback<T> extends ServerRequestCallback<T>
   {
      public RequestCallback(String progressMessage)
      {
         indicator_ = new GlobalProgressDelayer(
            globalDisplay_,  500, progressMessage).getIndicator();
      }
      
      @Override
      public void onResponseReceived(T response)
      {
         indicator_.onCompleted();
         onSuccess(response);
      }
      
      protected abstract void onSuccess(T response);

      @Override
      public void onError(ServerError error)
      {
         indicator_.onError(error.getUserMessage());
      }
      
      private ProgressIndicator indicator_;   
   };
   
   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private final CompilePdfServerOperations server_;
}
