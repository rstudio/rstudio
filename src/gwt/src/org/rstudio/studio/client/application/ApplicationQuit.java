/*
 * ApplicationQuit.java
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

import java.util.ArrayList;

import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.HandleUnsavedChangesEvent;
import org.rstudio.studio.client.application.events.QuitInitiatedEvent;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedEvent;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.application.model.SuspendOptions;
import org.rstudio.studio.client.application.model.TutorialApiCallContext;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.TimedProgressIndicator;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionOpener;
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog.Result;
import org.rstudio.studio.client.workbench.views.jobs.model.JobManager;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.terminal.TerminalHelper;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ApplicationQuit implements SaveActionChangedEvent.Handler,
                                        HandleUnsavedChangesEvent.Handler,
                                        SuspendAndRestartEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, ApplicationQuit> {}
   
   @Inject
   public ApplicationQuit(ApplicationServerOperations server,
                          GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          WorkbenchContext workbenchContext,
                          Provider<Source> pSource,
                          Provider<UserPrefs> pUiPrefs,
                          Commands commands,
                          Binder binder,
                          TerminalHelper terminalHelper,
                          Provider<JobManager> pJobManager,
                          Provider<SessionOpener> pSessionOpener)
   {
      // save references
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      workbenchContext_ = workbenchContext;
      pSource_ = pSource;
      pUserPrefs_ = pUiPrefs;
      terminalHelper_ = terminalHelper;
      pJobManager_ = pJobManager;
      pSessionOpener_ = pSessionOpener;
      
      // bind to commands
      binder.bind(commands, this);
      
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
      prepareForQuit(caption, true /*allowCancel*/, false /*forceSaveAll*/, quitContext);
   }

   public void prepareForQuit(final String caption,
                              final boolean allowCancel,
                              final boolean forceSaveAll,
                              final QuitContext quitContext)
   {
      String busyMode = pUserPrefs_.get().busyDetection().getValue();

      boolean busy = workbenchContext_.isServerBusy() || terminalHelper_.warnBeforeClosing(busyMode);
      String msg = null;
      if (busy)
      {
         if (workbenchContext_.isServerBusy() && !terminalHelper_.warnBeforeClosing(busyMode))
            msg = constants_.rSessionCurrentlyBusyMessage();
         else if (workbenchContext_.isServerBusy() && terminalHelper_.warnBeforeClosing(busyMode))
            msg = constants_.rSessionTerminalBusyMessage();
         else 
            msg = constants_.terminalCurrentlyBusyMessage();
      }

      eventBus_.fireEvent(new QuitInitiatedEvent());
      
      if (busy && !forceSaveAll)
      {
         if (allowCancel)
         {
            globalDisplay_.showYesNoMessage(
                  MessageDialog.QUESTION,
                  caption, 
                  constants_.applicationQuitMessage(msg),
                  () -> handleUnfinishedWork(caption, allowCancel, forceSaveAll, quitContext),
                  true);
         }
         else
         {
            handleUnfinishedWork(caption, allowCancel, forceSaveAll, quitContext);
         }
      }
      else
      {
         // if we aren't restoring source documents then close them all now
         if (pSource_.get() != null && !pUserPrefs_.get().restoreSourceDocuments().getValue())
         {
            pSource_.get().closeAllSourceDocs(caption,
                  () -> handleUnfinishedWork(caption, allowCancel, forceSaveAll, quitContext),
                  null);
         }
         else
         {
            handleUnfinishedWork(caption, allowCancel, forceSaveAll, quitContext);
         }
      }
   }
   
   private void handleUnfinishedWork(String caption, 
                                     boolean allowCancel,
                                     boolean forceSaveAll,
                                     QuitContext quitContext)
   {
      Command handleUnsaved = () -> {
         // handle unsaved editor changes
         handleUnsavedChanges(saveAction_.getAction(), caption, allowCancel, forceSaveAll,
               pSource_.get(), workbenchContext_, globalEnvTarget_, quitContext);
      };

      if (allowCancel)
      {
         // check for running jobs
         pJobManager_.get().promptForTermination((confirmed) -> 
         {
            if (confirmed)
            {
               handleUnsaved.execute();
            }
         });
      }
      else
      {
         handleUnsaved.execute();
      }
   }
   
   
   private static boolean handlingUnsavedChanges_;
   public static boolean isHandlingUnsavedChanges()
   {
      return handlingUnsavedChanges_;
   }
   
   public static void handleUnsavedChanges(final int saveAction, 
                                     String caption,
                                     boolean allowCancel,
                                     boolean forceSaveAll,
                                     final Source source,
                                     final WorkbenchContext workbenchContext,
                                     final UnsavedChangesTarget globalEnvTarget,
                                     final QuitContext quitContext)
   {   
      // see what the unsaved changes situation is and prompt accordingly
      ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
                        source.getUnsavedChanges(Source.TYPE_FILE_BACKED);
      
      // force save all
      if (forceSaveAll)
      {
         // save all unsaved documents and then quit
         source.handleUnsavedChangesBeforeExit(
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
                  constants_.quitRSessionsMessage(),
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
         String prompt = constants_.saveWorkspaceImageMessage() +
                         workbenchContext.getREnvironmentPath() + "?";
         RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
               GlobalDisplay.MSG_QUESTION,
               caption,
               prompt,
               allowCancel,
               () -> quitContext.onReadyToQuit(true),
               () -> quitContext.onReadyToQuit(false),
               () -> {},
               constants_.saveYesLabel(),
               constants_.saveNoLabel(),
               true);        
      }
      
      // a single unsaved document (can be any document in desktop mode, but 
      // must be from the main window in web mode)
      else if (saveAction != SaveAction.SAVEASK && 
               unsavedSourceDocs.size() == 1 &&
               (Desktop.hasDesktopFrame() ||
                !(unsavedSourceDocs.get(0) instanceof UnsavedChangesItem)))
      {
         source.saveWithPrompt(
           unsavedSourceDocs.get(0), 
           source.revertUnsavedChangesBeforeExitCommand(new Command() {
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
         ArrayList<UnsavedChangesTarget> unsaved = new ArrayList<>();
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
                  source.handleUnsavedChangesBeforeExit(
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
   
   public void performQuit(TutorialApiCallContext callContext, boolean saveChanges)
   {
      performQuit(callContext, saveChanges, null, null);
   }
   
   public void performQuit(TutorialApiCallContext callContext,
                           boolean saveChanges,
                           Command onQuitAcknowledged)
   {
      performQuit(callContext, null, saveChanges, null, null, onQuitAcknowledged);
   }
   
   public void performQuit(TutorialApiCallContext callContext,
                           boolean saveChanges,
                           String switchToProject)
   {
      performQuit(callContext, saveChanges, switchToProject, null);
   }
   
   public void performQuit(TutorialApiCallContext callContext,
                           boolean saveChanges,
                           String switchToProject,
                           RVersionSpec switchToRVersion)
   {
      performQuit(callContext, null, saveChanges, switchToProject, switchToRVersion);
   }
   
   public void performQuit(TutorialApiCallContext callContext,
                           String progressMessage,
                           boolean saveChanges, 
                           String switchToProject,
                           RVersionSpec switchToRVersion)
   {
      performQuit(callContext,
                  progressMessage,
                  saveChanges, 
                  switchToProject, 
                  switchToRVersion,
                  null);
   }
   
   public void performQuit(TutorialApiCallContext callContext,
                           String progressMessage,
                           boolean saveChanges, 
                           String switchToProject,
                           RVersionSpec switchToRVersion,
                           Command onQuitAcknowledged)
   {
      new QuitCommand(callContext,
                      progressMessage,
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
            DesktopFrameHelper.setPendingQuit(DesktopFrame.PENDING_QUIT_AND_EXIT, () ->
            {
               server_.handleUnsavedChangesCompleted(
                     handled_, 
                     new VoidServerRequestCallback());  
            });
         }
         
         private final boolean handled_;
      }
      
      // get unsaved source docs
      ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
                        pSource_.get().getUnsavedChanges(Source.TYPE_FILE_BACKED);
      
      if (unsavedSourceDocs.size() == 1)
      {
         pSource_.get().saveWithPrompt(
               unsavedSourceDocs.get(0), 
               pSource_.get().revertUnsavedChangesBeforeExitCommand(
                  new HandleUnsavedCommand(true)),
               new HandleUnsavedCommand(false));
      }
      else if (unsavedSourceDocs.size() > 1)
      {
         new UnsavedChangesDialog(
               constants_.quitRSessionTitle(),
               unsavedSourceDocs,
               new OperationWithInput<UnsavedChangesDialog.Result>() {
                  @Override
                  public void execute(Result result)
                  {
                     // save specified documents and then quit
                     pSource_.get().handleUnsavedChangesBeforeExit(
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
         // check for running jobs
         pJobManager_.get().promptForTermination((confirmed) ->
         {
            if (confirmed)
            {
               terminalHelper_.warnBusyTerminalBeforeCommand(() ->
               {
                  boolean saveChanges = saveAction_.getAction() != SaveAction.NOSAVE;
                  eventBus_.fireEvent(new SuspendAndRestartEvent(
                        SuspendOptions.createSaveMinimal(saveChanges),
                        null));
               }, constants_.restartRCaption(), constants_.terminalJobTerminatedQuestion(),
                  pUserPrefs_.get().busyDetection().getValue());
            }
         });
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
      if (suspendingAndRestarting_)
         return;
      
      // set restart pending for desktop
      DesktopFrameHelper.setPendingQuit(DesktopFrame.PENDING_QUIT_AND_RESTART, () ->
      {
         onSuspendAndRestartImpl(event);
      });
   }
   
   private void onSuspendAndRestartImpl(final SuspendAndRestartEvent event)
   {
      final TimedProgressIndicator progress = new TimedProgressIndicator(
            globalDisplay_.getProgressIndicator(constants_.progressErrorCaption()));
      progress.onTimedProgress(constants_.restartingRMessage(), 1000);
      
      final Operation onRestartComplete = () -> {
         suspendingAndRestarting_ = false;
         progress.onCompleted();
         eventBus_.fireEvent(new RestartStatusEvent(RestartStatusEvent.RESTART_COMPLETED));
      };

      // perform the suspend and restart
      suspendingAndRestarting_ = true;
      eventBus_.fireEvent(new RestartStatusEvent(RestartStatusEvent.RESTART_INITIATED));
      pSessionOpener_.get().suspendForRestart(
         event.getAfterRestartCommand(),
         event.getSuspendOptions(),
         () -> { // success
            onRestartComplete.execute();
         }, () -> { // failure
            onRestartComplete.execute();
            DesktopFrameHelper.setPendingQuit(DesktopFrame.PENDING_QUIT_NONE, () -> {});
         });
   }
   
   @Handler
   public void onQuitSession()
   {
      prepareForQuit(constants_.quitRSessionCaption(), (boolean saveChanges) -> performQuit(null, saveChanges));
   }

   @Handler
   public void onForceQuitSession()
   {
      prepareForQuit(
            constants_.quitRSessionCaption(),
            false /*allowCancel*/,
            false /*forceSaveChanges*/,
            (boolean saveChanges) -> performQuit(null, saveChanges));
   }

   public void doRestart(Session session)
   {
      prepareForQuit(constants_.restartRStudio(), (saveChanges) ->
      {
         final String project = StringUtil.nullCoalesce(
               session.getSessionInfo().getActiveProjectFile(),
               Projects.NONE);
         
         // NOTE: we don't use the 'switchToProject' variant here as we
         // want to open a brand new RStudio window for this project, rather
         // than just reload the current window with the requested project
         // now active
         performQuit(null, saveChanges, () ->
         {
            eventBus_.fireEvent(new OpenProjectNewWindowEvent(project, null));
         });
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
      public FileIcon getIcon()
      {
         return FileIcon.RDATA_ICON;
      }

      @Override
      public String getTitle()
      {
         return constants_.studioClientTitle();
      }

      @Override
      public String getPath()
      {
         return workbenchContext_.getREnvironmentPath();
      }
      
   };
   
   private String buildSwitchMessage(String switchToProject)
   {
      String msg = switchToProject != "none" ?
        constants_.switchingToProjectMessage() +
           FileSystemItem.createFile(switchToProject).getParentPathString() :
        constants_.closingProjectMessage();
      return msg + "...";
   }
   
   private class QuitCommand implements Command 
   {
      public QuitCommand(TutorialApiCallContext callContext,
                         String progressMessage,
                         boolean saveChanges, 
                         String switchToProject,
                         RVersionSpec switchToRVersion,
                         Command onQuitAcknowledged)
      {
         callContext_ = callContext;
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
                                    constants_.quitRSessionMessage();
         }
         
         final GlobalProgressDelayer progress = new GlobalProgressDelayer(
                                                               globalDisplay_,
                                                               250,
                                                               msg);
         
         // Use a barrier and LastChanceSaveEvent to allow source documents
         // and client state to be synchronized before quitting.
         Barrier barrier = new Barrier();
         barrier.addBarrierReleasedHandler((releasedEvent) ->
         {
            // All last chance save operations have completed (or possibly
            // failed). Now do the real quit.

            // notify the desktop frame that we are about to quit
            final int quitType = switchToProject_ == null
                  ? DesktopFrame.PENDING_QUIT_AND_EXIT
                  : DesktopFrame.PENDING_QUIT_RESTART_AND_RELOAD;
            
            DesktopFrameHelper.setPendingQuit(quitType, () ->
            {
               server_.quitSession(
                     saveChanges_,
                     switchToProject_,
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

                              if (callContext_ != null)
                              {
                                 eventBus_.fireEvent(new ApplicationTutorialEvent(
                                       ApplicationTutorialEvent.API_SUCCESS, callContext_));
                              }

                              // fire onQuitAcknowledged
                              if (onQuitAcknowledged_ != null)
                                 onQuitAcknowledged_.execute();
                           }
                           else
                           {
                              onFailedToQuit(constants_.serverQuitSession());
                           }
                        }

                        @Override
                        public void onError(ServerError error)
                        {
                           onFailedToQuit(error.getMessage());
                        }

                        private void onFailedToQuit(String message)
                        {
                           progress.dismiss();

                           if (callContext_ != null)
                           {
                              eventBus_.fireEvent(new ApplicationTutorialEvent(
                                    ApplicationTutorialEvent.API_ERROR,
                                    message,
                                    callContext_));
                           }
                           
                           DesktopFrameHelper.setPendingQuit(DesktopFrame.PENDING_QUIT_NONE, () -> {});
                        }
                     });
            });
            
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

      private final TutorialApiCallContext callContext_;
      private final boolean saveChanges_;
      private final String switchToProject_;
      private final RVersionSpec switchToRVersion_;
      private final String progressMessage_;
      private final Command onQuitAcknowledged_;
   }
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
   private boolean suspendingAndRestarting_ = false;

   // injected
   private final ApplicationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Provider<UserPrefs> pUserPrefs_;
   private final EventBus eventBus_;
   private final WorkbenchContext workbenchContext_;
   private final Provider<Source> pSource_;
   private final TerminalHelper terminalHelper_;
   private final Provider<JobManager> pJobManager_;
   private final Provider<SessionOpener> pSessionOpener_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
