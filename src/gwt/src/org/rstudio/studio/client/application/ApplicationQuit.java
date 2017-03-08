/*
 * ApplicationQuit.java
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

import java.util.ArrayList;

import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.BarrierReleasedEvent;
import org.rstudio.core.client.events.BarrierReleasedHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.HandleUnsavedChangesEvent;
import org.rstudio.studio.client.application.events.HandleUnsavedChangesHandler;
import org.rstudio.studio.client.application.events.QuitInitiatedEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedHandler;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.application.events.SuspendAndRestartHandler;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.application.model.SuspendOptions;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.common.TimedProgressIndicator;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog.Result;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleRestartRCompletedEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourceShim;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ApplicationQuit implements SaveActionChangedHandler,
                                        HandleUnsavedChangesHandler,
                                        SuspendAndRestartHandler
{
   public interface Binder extends CommandBinder<Commands, ApplicationQuit> {}
   
   @Inject
   public ApplicationQuit(ApplicationServerOperations server,
                          GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          WorkbenchContext workbenchContext,
                          SourceShim sourceShim,
                          Provider<UIPrefs> pUiPrefs,
                          Commands commands,
                          Binder binder)
   {
      // save references
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      workbenchContext_ = workbenchContext;
      sourceShim_ = sourceShim;
      pUiPrefs_ = pUiPrefs;
      
      // bind to commands
      binder.bind(commands, this);
      
      // only enable suspendSession() in devmode
      commands.suspendSession().setVisible(SuperDevMode.isActive());
      
      // subscribe to events
      eventBus.addHandler(SaveActionChangedEvent.TYPE, this);   
      eventBus.addHandler(HandleUnsavedChangesEvent.TYPE, this);
      eventBus.addHandler(SuspendAndRestartEvent.TYPE, this);
   }
   
  
   // notification that we are ready to quit
   public interface QuitContext
   {
      void onReadyToQuit(boolean saveChanges);
   }
   
   public void prepareForQuit(final String caption,
                              final QuitContext quitContext)
   {
      prepareForQuit(caption, false, quitContext);
   }
   
   public void prepareForQuit(final String caption,
                              final boolean forceSaveAll,
                              final QuitContext quitContext)
   {
      boolean busy = workbenchContext_.isServerBusy() || workbenchContext_.isTerminalBusy();
      String msg = null;
      if (busy)
      {
         if (workbenchContext_.isServerBusy() && !workbenchContext_.isTerminalBusy())
            msg = "The R session is currently busy.";
         else if (workbenchContext_.isServerBusy() && workbenchContext_.isTerminalBusy())
            msg = "The R session and the terminal are currently busy.";
         else 
            msg = "The terminal is currently busy.";
      }

      eventBus_.fireEvent(new QuitInitiatedEvent());
      
      if (busy && !forceSaveAll)
      {
         globalDisplay_.showYesNoMessage(
               MessageDialog.QUESTION,
               caption, 
               msg + " Are you sure you want to quit?",
               new Operation() {
                  @Override
                  public void execute()
                  {
                     handleUnsavedChanges(caption, forceSaveAll, quitContext);
                  }}, 
               true);
      }
      else
      {
         // if we aren't restoring source documents then close them all now
         if (!pUiPrefs_.get().restoreSourceDocuments().getValue())
         {
            sourceShim_.closeAllSourceDocs(caption, new Command() {
               @Override
               public void execute()
               {
                  handleUnsavedChanges(caption, forceSaveAll, quitContext);
               }
            });
         }
         else
         {
            handleUnsavedChanges(caption, forceSaveAll, quitContext);
         }
      }
   }
   
   private void handleUnsavedChanges(String caption, 
                                     boolean forceSaveAll,
                                     QuitContext quitContext)
   {
      handleUnsavedChanges(saveAction_.getAction(), caption, forceSaveAll,
            sourceShim_, workbenchContext_, globalEnvTarget_, quitContext);
   }
   
   
   private static boolean handlingUnsavedChanges_;
   public static boolean isHandlingUnsavedChanges()
   {
      return handlingUnsavedChanges_;
   }
   
   public static void handleUnsavedChanges(final int saveAction, 
                                     String caption,
                                     boolean forceSaveAll,
                                     final SourceShim sourceShim,
                                     final WorkbenchContext workbenchContext,
                                     final UnsavedChangesTarget globalEnvTarget,
                                     final QuitContext quitContext)
   {   
      // see what the unsaved changes situation is and prompt accordingly
      ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
                        sourceShim.getUnsavedChanges(Source.TYPE_FILE_BACKED);
      
      // force save all
      if (forceSaveAll)
      {
         // save all unsaved documents and then quit
         sourceShim.handleUnsavedChangesBeforeExit(
               unsavedSourceDocs,
               new Command() {
                  @Override
                  public void execute()
                  {
                     boolean saveChanges = saveAction != SaveAction.NOSAVE;
                     quitContext.onReadyToQuit(saveChanges);
                  }
               });
         
         return;
      }
      // no unsaved changes at all
      else if (saveAction != SaveAction.SAVEASK && unsavedSourceDocs.size() == 0)
      {
         // define quit operation
         final Operation quitOperation = new Operation() { public void execute() 
         {
            quitContext.onReadyToQuit(saveAction == SaveAction.SAVE);
         }};
        
         // if this is a quit session then we always prompt
         if (ApplicationAction.isQuit())
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
                  MessageDialog.QUESTION,
                  caption,
                  "Are you sure you want to quit the R session?",
                  quitOperation,
                  true);
         }
         else
         {
            quitOperation.execute();
         }
         
         return;
      }
      
      // just an unsaved environment
      if (unsavedSourceDocs.size() == 0 && workbenchContext != null) 
      {        
         // confirm quit and do it
         String prompt = "Save workspace image to " + 
                         workbenchContext.getREnvironmentPath() + "?";
         RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
               GlobalDisplay.MSG_QUESTION,
               caption,
               prompt,
               true,
               new Operation() { public void execute()
               {
                  quitContext.onReadyToQuit(true);      
               }},
               new Operation() { public void execute()
               {
                  quitContext.onReadyToQuit(false);
               }},
               new Operation() { public void execute()
               {
               }},
               "Save",
               "Don't Save",
               true);        
      }
      
      // a single unsaved document (can be any document in desktop mode, but 
      // must be from the main window in web mode)
      else if (saveAction != SaveAction.SAVEASK && 
               unsavedSourceDocs.size() == 1 &&
               (Desktop.isDesktop() || 
                !(unsavedSourceDocs.get(0) instanceof UnsavedChangesItem)))
      {
         sourceShim.saveWithPrompt(
           unsavedSourceDocs.get(0), 
           sourceShim.revertUnsavedChangesBeforeExitCommand(new Command() {
               @Override
               public void execute()
               {
                  quitContext.onReadyToQuit(saveAction == SaveAction.SAVE);
               }}),
           null);
      }
      
      // multiple save targets
      else
      {
         ArrayList<UnsavedChangesTarget> unsaved = 
                                      new ArrayList<UnsavedChangesTarget>();
         if (saveAction == SaveAction.SAVEASK && globalEnvTarget != null)
            unsaved.add(globalEnvTarget);
         unsaved.addAll(unsavedSourceDocs);
         new UnsavedChangesDialog(
            caption,
            unsaved,
            new OperationWithInput<UnsavedChangesDialog.Result>() {

               @Override
               public void execute(Result result)
               {
                  ArrayList<UnsavedChangesTarget> saveTargets =
                                                result.getSaveTargets();       
                  
                  // remote global env target from list (if specified) and 
                  // compute the saveChanges value
                  boolean saveGlobalEnv = saveAction == SaveAction.SAVE;
                  if (saveAction == SaveAction.SAVEASK && 
                      globalEnvTarget != null)
                     saveGlobalEnv = saveTargets.remove(globalEnvTarget);
                  final boolean saveChanges = saveGlobalEnv;
                  
                  // save specified documents and then quit
                  sourceShim.handleUnsavedChangesBeforeExit(
                        saveTargets,
                        new Command() {
                           @Override
                           public void execute()
                           {
                              quitContext.onReadyToQuit(saveChanges);
                           }
                        });
               }
            },
            
            // no cancel operation
            null
            ).showModal();
      }
   }
   
   public void performQuit(boolean saveChanges)
   {
      performQuit(saveChanges, null, null);
   }
   
   public void performQuit(boolean saveChanges, 
                           String switchToProject)
   {
      performQuit(saveChanges, switchToProject, null);
   }
   
   public void performQuit(boolean saveChanges, 
                           String switchToProject,
                           RVersionSpec switchToRVersion)
   {
      performQuit(null, saveChanges, switchToProject, switchToRVersion);
   }
   
   public void performQuit(String progressMessage,
                           boolean saveChanges, 
                           String switchToProject,
                           RVersionSpec switchToRVersion)
   {
      performQuit(progressMessage, 
                  saveChanges, 
                  switchToProject, 
                  switchToRVersion,
                  null);
   }
   
   public void performQuit(String progressMessage,
                           boolean saveChanges, 
                           String switchToProject,
                           RVersionSpec switchToRVersion,
                           Command onQuitAcknowledged)
   {
      new QuitCommand(progressMessage, 
                      saveChanges, 
                      switchToProject,
                      switchToRVersion,
                      onQuitAcknowledged).execute();
   }
   
   @Override
   public void onSaveActionChanged(SaveActionChangedEvent event)
   {
      saveAction_ = event.getAction();
   }
   
   @Override
   public void onHandleUnsavedChanges(HandleUnsavedChangesEvent event)
   {
      // command which will be used to callback the server
      class HandleUnsavedCommand implements Command
      {
         public HandleUnsavedCommand(boolean handled)
         {
            handled_ = handled;
         }
         
         @Override
         public void execute()
         {
            // this codepath is for when the user quits R using the q() 
            // function -- in this case our standard client quit codepath
            // isn't invoked, and as a result the desktop is not notified
            // that there is a pending quit (so thinks R has crashed when
            // the process exits). since this codepath is only for the quit
            // case (and not the restart or restart and reload cases)
            // we can set the pending quit bit here
            if (Desktop.isDesktop())
            {
               Desktop.getFrame().setPendingQuit(
                        DesktopFrame.PENDING_QUIT_AND_EXIT);
            }
            
            server_.handleUnsavedChangesCompleted(
                                          handled_, 
                                          new VoidServerRequestCallback());  
         }
         
         private final boolean handled_;
      };
      
      // get unsaved source docs
      ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
                        sourceShim_.getUnsavedChanges(Source.TYPE_FILE_BACKED);
      
      if (unsavedSourceDocs.size() == 1)
      {
         sourceShim_.saveWithPrompt(
               unsavedSourceDocs.get(0), 
               sourceShim_.revertUnsavedChangesBeforeExitCommand(
                                             new HandleUnsavedCommand(true)),
               new HandleUnsavedCommand(false));
      }
      else if (unsavedSourceDocs.size() > 1)
      {
         new UnsavedChangesDialog(
               "Quit R Session",
               unsavedSourceDocs,
               new OperationWithInput<UnsavedChangesDialog.Result>() {
                  @Override
                  public void execute(Result result)
                  {
                     // save specified documents and then quit
                     sourceShim_.handleUnsavedChangesBeforeExit(
                           result.getSaveTargets(),
                           new HandleUnsavedCommand(true));
                  }
                },
                new HandleUnsavedCommand(false)
         ).showModal();
      }
      else
      {
         new HandleUnsavedCommand(true).execute();
      }
   }
      
     
   @Handler
   public void onRestartR()
   {   
      boolean saveChanges = saveAction_.getAction() != SaveAction.NOSAVE;
      eventBus_.fireEvent(new SuspendAndRestartEvent(
                                 SuspendOptions.createSaveMinimal(saveChanges),
                                 null));  

   }
   
   @Handler
   public void onSuspendSession()
   {
      server_.suspendSession(true, new VoidServerRequestCallback());
   }
   
   @Override
   public void onSuspendAndRestart(final SuspendAndRestartEvent event)
   {
      // Ignore nested restarts once restart starts
      if (suspendingAndRestarting_) return;
      
      // set restart pending for desktop
      setPendinqQuit(DesktopFrame.PENDING_QUIT_AND_RESTART);
      
      final TimedProgressIndicator progress = new TimedProgressIndicator(
            globalDisplay_.getProgressIndicator("Error"));
      progress.onTimedProgress("Restarting R", 1000);
      
      final Operation onRestartComplete = new Operation() {
         @Override
         public void execute()
         {
            suspendingAndRestarting_ = false;
            progress.onCompleted();
         }
      };

      // perform the suspend and restart
      suspendingAndRestarting_ = true;
      eventBus_.fireEvent(
                  new RestartStatusEvent(RestartStatusEvent.RESTART_INITIATED));
      server_.suspendForRestart(event.getSuspendOptions(),
                                new VoidServerRequestCallback() {
         @Override 
         protected void onSuccess()
         { 
            // send pings until the server restarts
            sendPing(event.getAfterRestartCommand(), 200, 25, new Command() {

               @Override
               public void execute()
               {
                  onRestartComplete.execute();
                  
                  eventBus_.fireEvent(new RestartStatusEvent(
                                    RestartStatusEvent.RESTART_COMPLETED));
                  
               }
               
            });
         }
         @Override
         protected void onFailure()
         {
            onRestartComplete.execute();
            
            eventBus_.fireEvent(
               new RestartStatusEvent(RestartStatusEvent.RESTART_COMPLETED));
            
            setPendinqQuit(DesktopFrame.PENDING_QUIT_NONE);
         }
      });    
      
   } 
   
   private void setPendinqQuit(int pendingQuit)
   {
      if (Desktop.isDesktop())
         Desktop.getFrame().setPendingQuit(pendingQuit);
   }
   
   private void sendPing(final String afterRestartCommand,
                         int delayMs, 
                         final int maxRetries,
                         final Command onCompleted)
   {  
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

         private int retries_ = 0;
         private boolean pingDelivered_ = false;
         private boolean pingInFlight_ = false;
         
         @Override
         public boolean execute()
         {
            // if we've already delivered the ping or our retry count
            // is exhausted then return false
            if (pingDelivered_ || (++retries_ > maxRetries))
               return false;
            
            if (!pingInFlight_)
            {
               pingInFlight_ = true;
               server_.ping(new VoidServerRequestCallback() {
                  @Override
                  protected void onSuccess()
                  {
                     pingInFlight_ = false;
                     
                     if (!pingDelivered_)
                     {
                        pingDelivered_ = true;
                        
                        // issue after restart command
                        if (!StringUtil.isNullOrEmpty(afterRestartCommand))
                        {
                           eventBus_.fireEvent(
                                 new SendToConsoleEvent(afterRestartCommand, 
                                                        true, true));
                        }
                        // otherwise make sure the console knows we 
                        // restarted (ensure prompt and set focus)
                        else 
                        {
                           eventBus_.fireEvent(
                                          new ConsoleRestartRCompletedEvent());
                        }
                     }
                     
                     if (onCompleted != null)
                        onCompleted.execute();
                  }
                  
                  @Override
                  protected void onFailure()
                  {
                     pingInFlight_ = false;
                     
                     if (onCompleted != null)
                        onCompleted.execute();
                  }
               });
            }
            
            // keep trying until the ping is delivered
            return true;
         }
         
      }, delayMs);
      
      
   }
   
   
   @Handler
   public void onQuitSession()
   {
      prepareForQuit("Quit R Session", new QuitContext() {
         public void onReadyToQuit(boolean saveChanges)
         {
            performQuit(saveChanges);
         }   
      });
   }
   
   private UnsavedChangesTarget globalEnvTarget_ = new UnsavedChangesTarget()
   {
      @Override
      public String getId()
      {
         return "F59C8727-3C63-41F4-989C-B1E1D47760E3";
      }

      @Override
      public ImageResource getIcon()
      {
         return new ImageResource2x(FileIconResources.INSTANCE.iconRdata2x()); 
      }

      @Override
      public String getTitle()
      {
         return "Workspace image (.RData)";
      }

      @Override
      public String getPath()
      {
         return workbenchContext_.getREnvironmentPath();
      }
      
   };
   
   private String buildSwitchMessage(String switchToProject)
   {
      String msg = !switchToProject.equals("none") ?
        "Switching to project " + 
           FileSystemItem.createFile(switchToProject).getParentPathString() :
        "Closing project";
      return msg + "...";
   }
   
   private class QuitCommand implements Command 
   { 
      public QuitCommand(String progressMessage, 
                         boolean saveChanges, 
                         String switchToProject,
                         RVersionSpec switchToRVersion,
                         Command onQuitAcknowledged)
      {
         progressMessage_ = progressMessage;
         saveChanges_ = saveChanges;
         switchToProject_ = switchToProject;
         switchToRVersion_ = switchToRVersion;
         onQuitAcknowledged_ = onQuitAcknowledged;
      }
      
      public void execute()
      {
         // show delayed progress
         String msg = progressMessage_;
         if (msg == null)
         {
            msg = switchToProject_ != null ? 
                                    buildSwitchMessage(switchToProject_) :
                                    "Quitting R Session...";
         }
         final GlobalProgressDelayer progress = new GlobalProgressDelayer(
                                                               globalDisplay_,
                                                               250,
                                                               msg);

         // Use a barrier and LastChanceSaveEvent to allow source documents
         // and client state to be synchronized before quitting.
         Barrier barrier = new Barrier();
         barrier.addBarrierReleasedHandler(new BarrierReleasedHandler()
         {
            public void onBarrierReleased(BarrierReleasedEvent event)
            {
               // All last chance save operations have completed (or possibly
               // failed). Now do the real quit.

               // notify the desktop frame that we are about to quit
               String switchToProject = new String(switchToProject_);
               if (Desktop.isDesktop())
               {
                  if (Desktop.getFrame().isCocoa() && 
                      switchToProject_ != null)
                  {
                     // on Cocoa there's an ugly intermittent crash that occurs 
                     // when we reload, so exit this instance and start a new
                     // one when switching projects
                     Desktop.getFrame().setPendingProject(switchToProject_);
                     
                     // Since we're going to be starting a new process we don't
                     // want to pass a switchToProject argument to quitSession
                     switchToProject = null;
                  }
                  else
                  {
                     Desktop.getFrame().setPendingQuit(switchToProject_ != null ?
                              DesktopFrame.PENDING_QUIT_RESTART_AND_RELOAD :
                              DesktopFrame.PENDING_QUIT_AND_EXIT);
                  }
               }
               
               server_.quitSession(
                  saveChanges_,
                  switchToProject,
                  switchToRVersion_,
                  GWT.getHostPageBaseURL(),
                  new ServerRequestCallback<Boolean>()
                  {
                     @Override
                     public void onResponseReceived(Boolean response)
                     {
                        if (response)
                        {
                           // clear progress only if we aren't switching projects
                           // (otherwise we want to leave progress up until
                           // the app reloads)
                           if (switchToProject_ == null)
                              progress.dismiss();
                           
                           // fire onQuitAcknowledged
                           if (onQuitAcknowledged_ != null)
                              onQuitAcknowledged_.execute();
                        }
                        else
                        {
                           onFailedToQuit();
                        }
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        onFailedToQuit();
                     }
                     
                     private void onFailedToQuit()
                     {
                        progress.dismiss();

                        if (Desktop.isDesktop())
                        {
                           Desktop.getFrame().setPendingQuit(
                                         DesktopFrame.PENDING_QUIT_NONE);
                        }
                     }
                  });
            }
         });

         // We acquire a token to make sure that the barrier doesn't fire before
         // all the LastChanceSaveEvent listeners get a chance to acquire their
         // own tokens.
         Token token = barrier.acquire();
         try
         {
            eventBus_.fireEvent(new LastChanceSaveEvent(barrier));
         }
         finally
         {
            token.release();
         }
      }

      private final boolean saveChanges_;
      private final String switchToProject_;
      private final RVersionSpec switchToRVersion_;
      private final String progressMessage_;
      private final Command onQuitAcknowledged_;

   };

   private final ApplicationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Provider<UIPrefs> pUiPrefs_;
   private final EventBus eventBus_;
   private final WorkbenchContext workbenchContext_;
   private final SourceShim sourceShim_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();

   private boolean suspendingAndRestarting_ = false;
}
