/*
 * ApplicationInterrupt.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application;

import java.util.List;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleBusyEvent;
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
public class ApplicationInterrupt implements ConsoleBusyEvent.Handler

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
      isBusy_ = false;
      pendingInterruptHandler_ = null;

      eventBus_.addHandler(ConsoleBusyEvent.TYPE, this);
      
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
         
         server_.interrupt(new ServerRequestCallback<Boolean>()
         {
            @Override
            public void onResponseReceived(Boolean response)
            {
               // Now with async rpc, the server responds right away to the interrupt so it's not
               // a reliable way to tell that the console is not busy anymore. We listen for the
               // the busy events and call the handler when we see it's not busy now.
               if (!isBusy_)
               {
                  eventBus_.fireEvent(new InterruptStatusEvent(
                        InterruptStatusEvent.INTERRUPT_COMPLETED));
                  finishInterrupt(handler);
               }
               else
               {
                  pendingInterrupt_ = true;
                  pendingInterruptHandler_ = handler;
               }
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
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
                          List<String> errorHandlerTypes,
                          String replacedWithHandlerType) {
      final String originalDebugType = errorManager_.getErrorHandlerType();
      
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
            constants_.terminateRMessage());
   }

   @Override
   public void onConsoleBusy(ConsoleBusyEvent event)
   {
      isBusy_ = event.isBusy();

      if (!isBusy_ && pendingInterrupt_)
      {
         eventBus_.fireEvent(new InterruptStatusEvent(
               InterruptStatusEvent.INTERRUPT_COMPLETED));
         finishInterrupt(pendingInterruptHandler_);
      }
   }
   
   private void showInterruptUnresponsiveDialog()
   {
      showTerminationDialog(
         constants_.terminationDialog(TERMINATION_CONSEQUENCE_MSG));
   }
   

   private void showTerminationDialog(String message)
   {  
      globalDisplay_.showYesNoMessage(
            MessageDialog.WARNING,
            constants_.terminateRCaption(),
            message,
            false, 
            new ProgressOperation() {
               
               @Override
               public void execute(ProgressIndicator indicator)
               {
                  DesktopFrameHelper.setPendingQuit(DesktopFrame.PENDING_QUIT_RESTART_AND_RELOAD, () ->
                  {
                     executeImpl(indicator);
                  });
               }
               
               private void executeImpl(ProgressIndicator indicator)
               {
                  // determine the next session project
                  String nextProj = pWorkbenchContext_.get().getActiveProjectFile();
                  if (nextProj == null)
                     nextProj = Projects.NONE;
                  
                  // force the abort
                  server_.abort(nextProj, new VoidServerRequestCallback(indicator) {
                     
                     @Override 
                     protected void onSuccess()
                     {
                        if (!Desktop.isDesktop())
                           eventBus_.fireEvent(new ReloadEvent());
                     }
                     
                     @Override
                     protected void onFailure()
                     {
                        DesktopFrameHelper.setPendingQuit(DesktopFrame.PENDING_QUIT_NONE, () -> {});
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
   
   private void finishInterrupt(InterruptHandler handler)
   {
      interruptRequestCounter_ = 0;
      interruptUnresponsiveTimer_.cancel(); 
      pendingInterrupt_ = false;
      pendingInterruptHandler_ = null;
      if (handler != null)
      {
         handler.onInterruptFinished();
      }
   }
   
   private int interruptRequestCounter_ = 0;
   private Timer interruptUnresponsiveTimer_ = null;
   private boolean isBusy_;
   private InterruptHandler pendingInterruptHandler_ = null;
   private boolean pendingInterrupt_ = false;
   
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Provider<WorkbenchContext> pWorkbenchContext_;
   private final ApplicationServerOperations server_;
   private final ErrorManager errorManager_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
   private final static String TERMINATION_CONSEQUENCE_MSG = constants_.terminationConsequenceMessage();
}
