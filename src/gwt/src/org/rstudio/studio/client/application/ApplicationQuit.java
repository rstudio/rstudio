package org.rstudio.studio.client.application;

import java.util.ArrayList;

import org.rstudio.core.client.Barrier;
import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.BarrierReleasedEvent;
import org.rstudio.core.client.events.BarrierReleasedHandler;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
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
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
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
   
   
   @Override
   public void onSaveActionChanged(SaveActionChangedEvent event)
   {
      saveAction_ = event.getAction();
   }
     
   @Handler
   public void onQuitSession()
   {
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
      public QuitCommand(boolean saveChanges)
      {
         saveChanges_ = saveChanges;
      }
      
      public void execute()
      {
         ProgressIndicator indicator =
            globalDisplay_.getProgressIndicator("Error Quitting R");
         new QuitOperation(saveChanges_).execute(indicator);
      }
      
      private final boolean saveChanges_;
   };
   
   // quit session operation paramaterized by whether we save changes
   class QuitOperation implements ProgressOperation
   {
      QuitOperation(boolean saveChanges)
      {
         saveChanges_ = saveChanges;
      }
      public void execute(ProgressIndicator indicator)
      {
         if (Desktop.isDesktop())
         {
            indicator.onCompleted();
            desktopQuitR(saveChanges_);
         }
         else
         {
            indicator.onProgress("Quitting R Session...");
            server_.quitSession(saveChanges_,
                             new VoidServerRequestCallback(indicator));
         }
      }
      private final boolean saveChanges_ ;
   }
   
   private void desktopQuitR(final boolean saveChanges)
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
   private final SourceShim sourceShim_;
   
   private SaveAction saveAction_ = SaveAction.saveAsk();
  
}
