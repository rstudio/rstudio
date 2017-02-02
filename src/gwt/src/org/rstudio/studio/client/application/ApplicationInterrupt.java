/*
 * ApplicationInterrupt.java
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

package org.rstudio.studio.client.application;

import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.debugging.ErrorManager;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ApplicationInterrupt
{
   public interface Binder extends CommandBinder<Commands, ApplicationInterrupt> {}
   
   public interface InterruptHandler
   {
      public void onInterruptFinished();
   }
   
   @Inject
   public ApplicationInterrupt(GlobalDisplay globalDisplay,
                               EventBus eventBus,
                               ApplicationServerOperations server,
                               Provider<WorkbenchContext> pWorkbenchContext,
                               Commands commands,
                               Binder binder,
                               ErrorManager errorManager)
   {
      // save references
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
      pWorkbenchContext_ = pWorkbenchContext;
      errorManager_ = errorManager;
      
      // bind to commands
      binder.bind(commands, this);

   }
   
   public void interruptR(final InterruptHandler handler)
   {
      if (interruptRequestCounter_ == 0)
      {
         interruptRequestCounter_ = 1;
         interruptUnresponsiveTimer_ = new Timer() {
            @Override
            public void run()
            {
               showInterruptUnresponsiveDialog(); 
            }
         };
         interruptUnresponsiveTimer_.schedule(10000);
         
         eventBus_.fireEvent(new InterruptStatusEvent(
               InterruptStatusEvent.INTERRUPT_INITIATED));
         server_.interrupt(new VoidServerRequestCallback() {
            @Override
            public void onSuccess()
            {
               eventBus_.fireEvent(new InterruptStatusEvent(
                     InterruptStatusEvent.INTERRUPT_COMPLETED));
               finishInterrupt(handler);
            }
            
            @Override
            public void onFailure()
            {
               finishInterrupt(handler);
            }
         });
      }
      else
      {
         interruptRequestCounter_++;
         
         if (interruptRequestCounter_ >= 3)
         {
            interruptUnresponsiveTimer_.cancel(); 
            showInterruptUnresponsiveDialog();
         }
      }
   }

   public void interruptR(final InterruptHandler handler,
                          List<Integer> errorHandlerTypes,
                          int replacedWithHandlerType) {
      final int originalDebugType = errorManager_.getErrorHandlerType();
      
      if (!errorHandlerTypes.contains(originalDebugType)) {
         interruptR(handler);
      }
      else {
         errorManager_.setDebugSessionHandlerType(
            replacedWithHandlerType,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  interruptR(new InterruptHandler() {
                     @Override
                     public void onInterruptFinished()
                     {
                        errorManager_.setDebugSessionHandlerType(
                           originalDebugType,
                           new ServerRequestCallback<Void>()
                           {
                              @Override
                              public void onResponseReceived(Void v)
                              {
                                 handler.onInterruptFinished();
                              }
                            
                              @Override
                              public void onError(ServerError error)
                              {
                                 Debug.log(error.getMessage());
                              }
                           }
                        );
                     }
                  });
               }
             
               @Override
               public void onError(ServerError error)
               {
                  Debug.log(error.getMessage());
               }
            }
         );
      }
   }
   
   @Handler
   void onInterruptR()
   {  
      interruptR(null);
   }
   
   
   @Handler
   public void onTerminateR()
   {
      showTerminationDialog(
            TERMINATION_CONSEQUENCE_MSG +
            "\n\n" +
            "Are you sure you want to terminate R?");
   }
   
   private void showInterruptUnresponsiveDialog()
   {
      showTerminationDialog(
         "R is not responding to your request to interrupt processing so " +
         "to stop the current operation you may need to terminate R entirely." +
         "\n\n" +
         TERMINATION_CONSEQUENCE_MSG +
         "\n\n" +
         "Do you want to terminate R now?");  
   }
   

   private void showTerminationDialog(String message)
   {  
      globalDisplay_.showYesNoMessage(
            MessageDialog.WARNING,
            "Terminate R", 
            message,
            false, 
            new ProgressOperation() {
               @Override
               public void execute(ProgressIndicator indicator)
               {
                  setPendinqQuit(DesktopFrame.PENDING_QUIT_RESTART_AND_RELOAD);
                        
                  // determine the next session project
                  String nextProj = pWorkbenchContext_.get()
                                                      .getActiveProjectFile();
                  if (nextProj == null)
                     nextProj = Projects.NONE;
                  
                  // force the abort
                  server_.abort(nextProj,
                                new VoidServerRequestCallback(indicator) {
                     @Override 
                     protected void onSuccess()
                     {
                        if (!Desktop.isDesktop())
                           eventBus_.fireEvent(new ReloadEvent());
                     }
                     @Override
                     protected void onFailure()
                     {
                        setPendinqQuit(DesktopFrame.PENDING_QUIT_NONE);
                     }
                  }); 
               } 
            },
            new ProgressOperation() {

               @Override
               public void execute(ProgressIndicator indicator)
               {
                  indicator.onCompleted();    
               } 
            }, 
            false);   
   }
   
   private void setPendinqQuit(int pendingQuit)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().setPendingQuit(pendingQuit);
   }
   
   private void finishInterrupt(InterruptHandler handler)
   {
      interruptRequestCounter_ = 0;
      interruptUnresponsiveTimer_.cancel(); 
      if (handler != null)
      {
         handler.onInterruptFinished();
      }
   }
   
   private int interruptRequestCounter_ = 0;
   private Timer interruptUnresponsiveTimer_ = null;
   
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Provider<WorkbenchContext> pWorkbenchContext_;
   private final ApplicationServerOperations server_;
   private final ErrorManager errorManager_;
   
   private final static String TERMINATION_CONSEQUENCE_MSG = 
      "Terminating R will cause your R session to immediately abort. " +
      "Active computations will be interrupted and unsaved source file " +
      "changes and workspace objects will be discarded.";
}
