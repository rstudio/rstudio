/*
 * BuildPresenter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.buildtools;

import java.util.ArrayList;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedHandler;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog.Result;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;
import org.rstudio.studio.client.workbench.views.source.SourceShim;

public class BuildPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
      void buildStarted();
      
      void showOutput(String output);
      void scrollToBottom();
      
      void buildCompleted();
      
      HasClickHandlers stopButton();
   }
   
   @Inject
   public BuildPresenter(Display display, 
                         GlobalDisplay globalDisplay,
                         SourceShim sourceShim,
                         UIPrefs uiPrefs,
                         BuildServerOperations server,
                         final Commands commands,
                         EventBus eventBus)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      sourceShim_ = sourceShim;
      uiPrefs_ = uiPrefs;
      eventBus_ = eventBus;
      commands_ = commands;
        
      eventBus.addHandler(BuildStartedEvent.TYPE, 
                          new BuildStartedEvent.Handler()
      {  
         @Override
         public void onBuildStarted(BuildStartedEvent event)
         {
            commands.stopBuild().setEnabled(true);
            
            view_.bringToFront();
            view_.buildStarted();
         
         }
      });
      
      eventBus.addHandler(BuildOutputEvent.TYPE, 
                          new BuildOutputEvent.Handler()
      {
         @Override
         public void onBuildOutput(BuildOutputEvent event)
         {
            view_.showOutput(event.getOutput());
         }
      });
      
      eventBus.addHandler(BuildCompletedEvent.TYPE, 
            new BuildCompletedEvent.Handler()
      {  
         @Override
         public void onBuildCompleted(BuildCompletedEvent event)
         {
            commands.stopBuild().setEnabled(false);
            
            view_.bringToFront();
            view_.buildCompleted();
            if (event.getRestartR())
            {
               eventBus_.fireEvent(
                  new SuspendAndRestartEvent(event.getAfterRestartCommand()));
            }  
         }
      });
      
      // invalidate devtools load all path whenever the project ui prefs
      // or working directory changes
      eventBus.addHandler(UiPrefsChangedEvent.TYPE, new UiPrefsChangedHandler() 
      {
         @Override
         public void onUiPrefsChanged(UiPrefsChangedEvent e)
         {
            if (e.getType().equals(UiPrefsChangedEvent.PROJECT_TYPE))
               devtoolsLoadAllPath_ = null;
         }
      });
      eventBus.addHandler(WorkingDirChangedEvent.TYPE, 
                          new WorkingDirChangedHandler() {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            devtoolsLoadAllPath_ = null;
         }      
      }); 
      
      
      view_.stopButton().addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            commands.stopBuild().execute();
         }
      });
   }
   
   public void initialize(BuildState buildState)
   {
      view_.buildStarted();
      view_.showOutput(buildState.getOutput());
      if (!buildState.isRunning())
         view_.buildCompleted();
      else
         commands_.stopBuild().setEnabled(true);
         
      
   }
   
   private void sendLoadCommandToConsole(String loadCommand)
   {
      eventBus_.fireEvent(new SendToConsoleEvent(loadCommand, true));
   }
   
  
   void onBuildAll()
   {
      startBuild("build-all");
   }
   
   void onDevtoolsLoadAll()
   {
      withDevtoolsLoadAllPath(new CommandWithArg<String>() {
         @Override
         public void execute(String loadAllPath)
         {
            sendLoadCommandToConsole(
                           "devtools::load_all(\"" + loadAllPath + "\")");
            commands_.activateConsole().execute();
         } 
      });
   }
   
   void onBuildSourcePackage()
   {
      startBuild("build-source-package");
   }
   
   void onBuildBinaryPackage()
   {
      startBuild("build-binary-package");
   }
   
   void onRoxygenizePackage()
   {
      startBuild("roxygenize-package");
   }
   
   void onCheckPackage()
   {
      startBuild("check-package");
   }
   
  
   void onRebuildAll()
   {
      startBuild("rebuild-all");
   }
   
   void onCleanAll()
   {
      startBuild("clean-all");
   }
   
   private void startBuild(final String type)
   {
      // attempt to start a build (this will be a silent no-op if there
      // is already a build running)
      final Command buildCommand = new Command() {
         @Override
         public void execute()
         {
            server_.startBuild(type, new SimpleRequestCallback<Boolean>() {
               @Override
               public void onResponseReceived(Boolean response)
               {
                 
               }
            });
         }
      };
      
      if (uiPrefs_.saveAllBeforeBuild().getValue())
      {
         sourceShim_.saveAllUnsaved(buildCommand);
      }
      else
      {
         String alwaysSaveOption = !uiPrefs_.saveAllBeforeBuild().getValue() ?
                                    "Always save files before build" : null;
         
         ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
               sourceShim_.getUnsavedChanges();

         if (unsavedSourceDocs.size() > 0)
         {
            new UnsavedChangesDialog(
                  "Build",
                  alwaysSaveOption,
                  unsavedSourceDocs,
                  new OperationWithInput<UnsavedChangesDialog.Result>() {
                     @Override
                     public void execute(Result result)
                     {
                        if (result.getAlwaysSave())
                        {
                           uiPrefs_.saveAllBeforeBuild().setGlobalValue(true);
                           uiPrefs_.writeUIPrefs();
                        }
                        
                        sourceShim_.handleUnsavedChangesBeforeExit(
                                                      result.getSaveTargets(),
                                                      buildCommand);
                        
                        
                     }
                   },
                   null
            ).showModal(); 
         }
         else
         {
            buildCommand.execute();
         }
      }
   }
   
   void onStopBuild()
   {
       server_.terminateBuild(new DelayedProgressRequestCallback<Boolean>(
                                                       "Terminating Build..."){
         @Override
         protected void onSuccess(Boolean response)
         {
            if (!response)
            {
               globalDisplay_.showErrorMessage(
                     "Error Terminating Build",
                     "Unable to terminate build. Please try again.");
            }
         }
       });  
   }
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      Scheduler.get().scheduleDeferred(new Command()
      {
         @Override
         public void execute()
         {
            view_.scrollToBottom();
         }
      });
   }
   
   private void withDevtoolsLoadAllPath(
                                 final CommandWithArg<String> onAvailable)
   {
      if (devtoolsLoadAllPath_ != null)
      {
         onAvailable.execute(devtoolsLoadAllPath_);
      }
      else
      {
         server_.devtoolsLoadAllPath(new SimpleRequestCallback<String>() {
            @Override
            public void onResponseReceived(String loadAllPath)
            {
               devtoolsLoadAllPath_ = loadAllPath;
               onAvailable.execute(devtoolsLoadAllPath_);
            }
         });
      }
      
   }
   
   private String devtoolsLoadAllPath_ = null;
   
   private final GlobalDisplay globalDisplay_;
   private final SourceShim sourceShim_;
   private final UIPrefs uiPrefs_;
   private final BuildServerOperations server_;
   private final Display view_ ; 
   private final EventBus eventBus_;
   private final Commands commands_;
}
