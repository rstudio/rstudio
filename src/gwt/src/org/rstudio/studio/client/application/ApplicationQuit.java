package org.rstudio.studio.client.application;

import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.BarrierReleasedEvent;
import org.rstudio.core.client.events.BarrierReleasedHandler;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SaveActionChangedEvent;
import org.rstudio.studio.client.application.events.SaveActionChangedHandler;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.LastChanceSaveEvent;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.views.source.SourceShim;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationQuit implements SaveActionChangedHandler
{
   public interface Binder extends CommandBinder<Commands, ApplicationQuit> {}
   
   @Inject
   public ApplicationQuit(ApplicationServerOperations server,
                          GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          WorkbenchContext workbenchContext,
                          SourceShim sourceShim,
                          Commands commands,
                          Binder binder)
   {
      // save references
      server_ = server;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      workbenchContext_ = workbenchContext;
      sourceShim_ = sourceShim;
      
      // bind to commands
      binder.bind(commands, this);
      
      // subscribe to events
      eventBus.addHandler(SaveActionChangedEvent.TYPE, this);   
   }
   
  
   // does whatever prompting is required before quit and then returns a 
   // command which can be executed to actually implement the quit
   public interface QuitContext
   {
      void onReadyToQuit(boolean saveChanges);
   }
   
   public void prepareForQuit(String caption,
                              final QuitContext quitContext)
   {
      // prompt only for unsaved environment (see below for an implementation
      // of unsaved prompting that also includes edited source documents -- 
      // note this implementation predated the quitContext.onReadyToQuit idiom
      // so if re-introduced would need to use that mechanism rather than
      // invoking a QuitOperation or QuitCommand directly.
      
      if (saveAction_.getAction() == SaveAction.SAVEASK)
      {
         // confirm quit and do it
         String prompt = "Save workspace image to " + 
                         workbenchContext_.getREnvironmentPath() + "?";
         globalDisplay_.showYesNoMessage(
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
      else
      {
         quitContext.onReadyToQuit(saveAction_.getAction() == SaveAction.SAVE);
      }
      
      
      /* 
       * NOTE: This is the implementation of unsaved change prompting for
       * edited files. This could be restored for either optional prompting
       * or required prompting. Note however if we do use this in preference
       * to the above then we should do a careful review of the 
       * Source.handleUnsavedChangesBefore exit method to make sure that 
       * the exit sequence behaves sanely. We also would need to implement
       * the unsaved change prompting for the q() code path (note we could
       * choose not to do this if the unsaved change prompting was optional).
       * We also need to make this interface work with the prepareForQuit
       * interface above (used by projects). this would just entail returing
       * the quitCommand to the prepareForQuit caller rather than executing
       * the quit directly
       * 
      // see what the unsaved changes situation is and prompt accordingly
      final int saveAction = saveAction_.getAction();
      ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
                                          sourceShim_.getUnsavedChanges();
      
      // no unsaved changes at all
      if (saveAction != SaveAction.SAVEASK && unsavedSourceDocs.size() == 0)
      {
         new QuitCommand(saveAction == SaveAction.SAVE).execute();
      }
      
      // just an unsaved environment
      else if (unsavedSourceDocs.size() == 0) 
      {    
         // confirm quit and do it
         String prompt = "Save workspace image to " + 
                         workbenchContext_.getREnvironmentPath() + "?";
         globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION,
                                         "Quit R Session",
                                         prompt,
                                         true,
                                         new QuitOperation(true),
                                         new QuitOperation(false),
                                         "Save",
                                         "Don't Save",
                                         true);   
      }
      
      // a single unsaved document
      else if (saveAction != SaveAction.SAVEASK && 
               unsavedSourceDocs.size() == 1)
      {
         QuitCommand quitCommand = 
                  new QuitCommand(saveAction == SaveAction.SAVE);
         sourceShim_.saveWithPrompt(unsavedSourceDocs.get(0), quitCommand);
      }
      
      // multiple save targets
      else
      {
         ArrayList<UnsavedChangesTarget> unsaved = 
                                      new ArrayList<UnsavedChangesTarget>();
         if (saveAction == SaveAction.SAVEASK)
            unsaved.add(globalEnvTarget_);
         unsaved.addAll(unsavedSourceDocs);
         UnsavedChangesDialog dlg = new UnsavedChangesDialog(
            "Quit R Session",
            unsaved,
            new OperationWithInput<ArrayList<UnsavedChangesTarget>>() {

               @Override
               public void execute(ArrayList<UnsavedChangesTarget> saveTargets)
               {
                  // remote global env target from list (if specified) and 
                  // create the appropriate quit command
                  boolean saveGlobalEnv = saveAction == SaveAction.SAVE;
                  if (saveAction == SaveAction.SAVEASK)
                     saveGlobalEnv = saveTargets.remove(globalEnvTarget_);
                  QuitCommand quitCommand = new QuitCommand(saveGlobalEnv);
                  
                  // save specified documents and then quit
                  sourceShim_.handleUnsavedChangesBeforeExit(saveTargets,
                                                             quitCommand);
               }
               
            });
         dlg.showModal();
      }
      */
   }
   
   public void performQuit(boolean saveChanges, String switchToProjectPath)
   {
      new QuitCommand(saveChanges, switchToProjectPath).execute();
   }
   
   @Override
   public void onSaveActionChanged(SaveActionChangedEvent event)
   {
      saveAction_ = event.getAction();
   }
     
   @Handler
   public void onQuitSession()
   {
      prepareForQuit("Quit R Session", new QuitContext() {
         public void onReadyToQuit(boolean saveChanges)
         {
            performQuit(saveChanges, null);
         }   
      });
   }
   
   
   @SuppressWarnings("unused")
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
         return FileIconResources.INSTANCE.iconRdata(); 
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
   
   private class QuitCommand implements Command 
   {
      public QuitCommand(boolean saveChanges, String switchToProjectPath)
      {
         saveChanges_ = saveChanges;
         switchToProjectPath_ = switchToProjectPath;
      }
      
      public void execute()
      {
         ProgressIndicator indicator =
            globalDisplay_.getProgressIndicator("Error Quitting R");
         
         if (Desktop.isDesktop())
         {
            indicator.onCompleted();
            desktopQuitR(saveChanges_, switchToProjectPath_);
         }
         else
         {
            indicator.onProgress("Quitting R Session...");
            server_.quitSession(saveChanges_,
                                switchToProjectPath_,
                                new VoidServerRequestCallback(indicator));
         }
      }
      
      private final boolean saveChanges_;
      private final String switchToProjectPath_;
      
   };
   
  
   private void desktopQuitR(final boolean saveChanges,
                             final String switchToProjectPath)
   {
      final GlobalProgressDelayer progress = new GlobalProgressDelayer(
            globalDisplay_,
            "Quitting R Session...");

      // Use a barrier and LastChanceSaveEvent to allow source documents
      // and client state to be synchronized before quitting.

      Barrier barrier = new Barrier();
      barrier.addBarrierReleasedHandler(new BarrierReleasedHandler()
      {
         public void onBarrierReleased(BarrierReleasedEvent event)
         {
            // All last chance save operations have completed (or possibly
            // failed). Now do the real quit.

            server_.quitSession(
                  saveChanges,
                  switchToProjectPath,
                  new VoidServerRequestCallback(
                        globalDisplay_.getProgressIndicator("Error Quitting R")) 
                  {

                     @Override
                     public void onResponseReceived(Void response)
                     {
                        progress.dismiss();
                        super.onResponseReceived(response);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        progress.dismiss();
                        super.onError(error);
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
 
   
   
   private final ApplicationServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final WorkbenchContext workbenchContext_;
   @SuppressWarnings("unused")
   private final SourceShim sourceShim_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
  
}
