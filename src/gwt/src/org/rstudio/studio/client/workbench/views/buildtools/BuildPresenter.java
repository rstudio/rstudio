/*
 * BuildPresenter.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.compile.CompileError;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.compile.errorlist.CompileErrorList;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedHandler;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildErrorsEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;
import org.rstudio.studio.client.workbench.views.source.SourceBuildHelper;

public class BuildPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
      void buildStarted();
      
      void showOutput(CompileOutput output);
      void scrollToBottom();
      
      void showErrors(String basePath,
                      JsArray<CompileError> errors, 
                      boolean ensureVisible,
                      int autoSelect);
      void buildCompleted();
      
      HasSelectionCommitHandlers<CodeNavigationTarget> errorList();
      
      HasClickHandlers stopButton();
   }
   
   @Inject
   public BuildPresenter(Display display, 
                         GlobalDisplay globalDisplay,
                         UIPrefs uiPrefs,
                         BuildServerOperations server,
                         final Commands commands,
                         EventBus eventBus,
                         FileTypeRegistry fileTypeRegistry,
                         SourceBuildHelper sourceBuildHelper)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      uiPrefs_ = uiPrefs;
      eventBus_ = eventBus;
      commands_ = commands;
      fileTypeRegistry_ = fileTypeRegistry;
      sourceBuildHelper_ = sourceBuildHelper;
        
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
      
      eventBus.addHandler(BuildErrorsEvent.TYPE, 
                          new BuildErrorsEvent.Handler()
      {         
         @Override
         public void onBuildErrors(BuildErrorsEvent event)
         {        
            view_.showErrors(event.getBaseDirectory(),
                             event.getErrors(), 
                             true,
                             uiPrefs_.navigateToBuildError().getValue() ?
                                 CompileErrorList.AUTO_SELECT_FIRST_ERROR :
                                 CompileErrorList.AUTO_SELECT_NONE);
            
            if (uiPrefs_.navigateToBuildError().getValue())
            {
               CompileError error = CompileError.getFirstError(event.getErrors());
               if (error != null)
               {
                  fileTypeRegistry_.editFile(
                    FileSystemItem.createFile(error.getPath()),
                    FilePosition.create(error.getLine(), error.getColumn()),
                    true);
               }
            }
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
      
      view_.errorList().addSelectionCommitHandler(
            new SelectionCommitHandler<CodeNavigationTarget>() {
         @Override
         public void onSelectionCommit(
                     SelectionCommitEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            FileSystemItem fsi = FileSystemItem.createFile(target.getFile());
            fileTypeRegistry_.editFile(fsi, target.getPosition());
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
      
      JsArray<CompileOutput> outputs = buildState.getOutputs();
      for (int i = 0; i<outputs.length(); i++)
         view_.showOutput(outputs.get(i));
      
      if (buildState.getErrors().length() > 0)
         view_.showErrors(buildState.getErrorsBaseDir(),
                          buildState.getErrors(), 
                          false,
                          CompileErrorList.AUTO_SELECT_NONE);
      
      if (!buildState.isRunning())
         view_.buildCompleted();
      else
         commands_.stopBuild().setEnabled(true);
         
      
   }
   
   private void sendLoadCommandToConsole(String loadCommand)
   {
      eventBus_.fireEvent(new SendToConsoleEvent(loadCommand, true, true));
   }
   
  
   void onBuildAll()
   {
      startBuild("build-all");
   }
   
   void onDevtoolsLoadAll()
   {
      sourceBuildHelper_.withSaveFilesBeforeCommand(new Command() {
         @Override
         public void execute()
         {
            withDevtoolsLoadAllPath(new CommandWithArg<String>() {
               @Override
               public void execute(String loadAllPath)
               {
                  sendLoadCommandToConsole(
                              "devtools::load_all(\"" + loadAllPath + "\")");
               } 
            }); 
         }
      }, "Build");
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
      sourceBuildHelper_.withSaveFilesBeforeCommand(new Command() {
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
      }, "Build");
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
   private final UIPrefs uiPrefs_;
   private final BuildServerOperations server_;
   private final Display view_ ; 
   private final EventBus eventBus_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final SourceBuildHelper sourceBuildHelper_;
}
