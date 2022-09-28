/*
 * BuildPresenter.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import com.google.inject.Provider;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.PrefLayer;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildErrorsEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildRenderSubTypeEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildServeSubTypeEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.jobs.model.JobManager;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.terminal.TerminalHelper;

public class BuildPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView 
   {
      void buildStarted();

      void showOutput(CompileOutput output, boolean tail);
      void scrollToBottom();

      void showErrors(String basePath,
                      JsArray<SourceMarker> errors,
                      boolean ensureVisible,
                      int autoSelect,
                      boolean openErrors,
                      String buildType);
      void buildCompleted();

      
      HasSelectionCommitHandlers<CodeNavigationTarget> errorList();
        
      HandlerRegistration addBuildRenderSubTypeHandler(BuildRenderSubTypeEvent.Handler handler);
      HandlerRegistration addBuildServeSubTypeHandler(BuildServeSubTypeEvent.Handler handler);
      

      HasClickHandlers stopButton();

      String errorsBuildType();
   }

   @Inject
   public BuildPresenter(Display display,
                         GlobalDisplay globalDisplay,
                         UserPrefs uiPrefs,
                         WorkbenchContext workbenchContext,
                         BuildServerOperations server,
                         final Commands commands,
                         EventBus eventBus,
                         FileTypeRegistry fileTypeRegistry,
                         Session session,
                         DependencyManager dependencyManager,
                         Source source,
                         TerminalHelper terminalHelper,
                         Provider<JobManager> pJobManager,
                         FilesServerOperations fileServer)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      userPrefs_ = uiPrefs;
      workbenchContext_ = workbenchContext;
      eventBus_ = eventBus;
      commands_ = commands;
      fileTypeRegistry_ = fileTypeRegistry;
      source_ = source;
      terminalHelper_ = terminalHelper;
      pJobManager_ = pJobManager;
      session_ = session;
      dependencyManager_ = dependencyManager;
      fileServer_ = fileServer;

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
            view_.showOutput(event.getOutput(), true);
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
                             userPrefs_.navigateToBuildError().getValue() ?
                                 SourceMarkerList.AUTO_SELECT_FIRST_ERROR :
                                 SourceMarkerList.AUTO_SELECT_NONE,
                             event.openErrorList(),
                             event.type());

            if (userPrefs_.navigateToBuildError().getValue() && event.openErrorList())
            {
               SourceMarker error = SourceMarker.getFirstError(event.getErrors());
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
            workbenchContext_.setBuildInProgress(false);

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
      eventBus.addHandler(UserPrefsChangedEvent.TYPE, new UserPrefsChangedEvent.Handler()
      {
         @Override
         public void onUserPrefsChanged(UserPrefsChangedEvent e)
         {
            if (e.getName() == PrefLayer.LAYER_USER)
               devtoolsLoadAllPath_ = null;
         }
      });
      eventBus.addHandler(WorkingDirChangedEvent.TYPE,
                          new WorkingDirChangedEvent.Handler() {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            devtoolsLoadAllPath_ = null;
         }
      });

      view_.errorList().addSelectionCommitHandler((SelectionCommitEvent<CodeNavigationTarget> event) ->
      {
         CodeNavigationTarget target = event.getSelectedItem();
         FileSystemItem fsi = FileSystemItem.createFile(target.getFile());

         if (view_.errorsBuildType() == "test-file" ||
             view_.errorsBuildType() == "test-shiny-file")
         {
            // for test files, we want to avoid throwing errors when the file is missing
            fileServer_.stat(target.getFile(), new ServerRequestCallback<FileSystemItem>()
            {
               @Override
               public void onResponseReceived(final FileSystemItem fsi)
               {
                  if (fsi.exists())
                     fileTypeRegistry_.editFile(fsi, target.getPosition());
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
         }
         else
         {
            fileTypeRegistry_.editFile(fsi, target.getPosition());
         }
      });

      view_.addBuildRenderSubTypeHandler(event -> {
         startBuild("build-all", event.getSubType());
      });
      
      view_.addBuildServeSubTypeHandler(event -> {
         quartoServe(event.getSubType(), false);
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
         view_.showOutput(outputs.get(i), false);

      if (buildState.getErrors().length() > 0)
         view_.showErrors(buildState.getErrorsBaseDir(),
                          buildState.getErrors(),
                          false,
                          SourceMarkerList.AUTO_SELECT_NONE,
                          true,
                          buildState.type());

      if (!buildState.isRunning())
         view_.buildCompleted();
      else
         commands_.stopBuild().setEnabled(true);

      view_.scrollToBottom();
   }

   private void sendLoadCommandToConsole(String loadCommand)
   {
      eventBus_.fireEvent(new SendToConsoleEvent(loadCommand, true, true));
   }


   void onBuildAll()
   {
      startBuild("build-all");
   }

   void onBuildIncremental()
   {
      startBuild("build-incremental");
   }

   void onBuildFull()
   {
      startBuild("build-full");
   }

   void onDevtoolsLoadAll()
   {
      source_.withSaveFilesBeforeCommand(() ->
      {
         withDevtoolsLoadAllPath(loadAllPath ->
         {
            sendLoadCommandToConsole("devtools::load_all(\"" + loadAllPath + "\")");
         });
      }, () -> {}, constants_.buildText());
   }
   
   void onServeQuartoSite()
   {
      quartoServe("default", false);
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
      dependencyManager_.withRoxygen(
            constants_.packageDocumentationProgressCaption(),
            constants_.packageDocumentationProgressCaption(),
            () -> startBuild("roxygenize-package"));
   }

   void onCheckPackage()
   {
      startBuild("check-package");
   }

   void onTestPackage()
   {
      startBuild("test-package");
   }

   void onTestTestthatFile()
   {
   }

   void onTestShinytestFile()
   {
   }

   void onCleanAll()
   {
      startBuild("clean-all");
   }

   private void startBuild(final String type)
   {
      startBuild(type, "");
   }

   private void startBuild(final String type, final String subType)
   {
      if (session_.getSessionInfo().getBuildToolsType() == SessionInfo.BUILD_TOOLS_WEBSITE)
      {
          dependencyManager_.withRMarkdown(constants_.buildingSitesUserAction(), new Command() {
            @Override
            public void execute()
            {
               executeBuild(type, subType);
            }
          });
      }
      else if (session_.getSessionInfo().getBuildToolsType() == SessionInfo.BUILD_TOOLS_QUARTO)
      {
         // books get a render and serve if the target is html or pdf
         QuartoConfig config = session_.getSessionInfo().getQuartoConfig();
         if (config.project_type.equals(SessionInfo.QUARTO_PROJECT_TYPE_BOOK))
         {
            if (shouldRenderAndServeBook(subType))
               quartoServe(subType, true);
            else
               executeBuild("build-all", subType);
         }
         else if (isQuartoSubProject(config) || !QuartoHelper.isQuartoWebsiteConfig(config))
         {
            executeBuild(type, "");
         }
         else 
         {
            quartoServe("default", true);
         }
      }
      else
      {
         executeBuild(type, subType);
      }
   }


   private void executeBuild(final String type, final String subType)
   {
      if (type != "build-all" || session_.getSessionInfo().getBuildToolsType() == SessionInfo.BUILD_TOOLS_QUARTO)
      {
         executeBuildNoBusyCheck(type, subType);
         return;
      }

      // check for running jobs
      pJobManager_.get().promptForTermination((confirmed) ->
      {
         if (confirmed)
         {
            terminalHelper_.warnBusyTerminalBeforeCommand(() ->
                  executeBuildNoBusyCheck(type, subType),
                  constants_.buildText(), constants_.terminalTerminatedQuestion(),
                  userPrefs_.busyDetection().getValue());
         }
      });

   }

   private void executeBuildNoBusyCheck(final String type, final String subType)
   {
      // attempt to start a build (this will be a silent no-op if there
      // is already a build running)
      workbenchContext_.setBuildInProgress(true);
      source_.withSaveFilesBeforeCommand(() ->
      {
         server_.startBuild(type, subType, new SimpleRequestCallback<Boolean>() {
            @Override
            public void onResponseReceived(Boolean response)
            {
            }

            @Override
            public void onError(ServerError error)
            {
               super.onError(error);
               workbenchContext_.setBuildInProgress(false);
            }

         });
      }, () -> {}, constants_.buildText());
   }

   void onStopBuild()
   {
       server_.terminateBuild(new DelayedProgressRequestCallback<Boolean>(
                                                       constants_.terminatingBuildMessage()){
         @Override
         protected void onSuccess(Boolean response)
         {
            if (!response)
            {
               globalDisplay_.showErrorMessage(
                     constants_.errorTerminatingBuildCaption(),
                     constants_.errorTerminatingBuildMessage());
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
   
   
   private boolean shouldRenderAndServeBook(String bookType)
   {
      // don't do a serve if this is a sub-project (as that might cause an over-render)
      QuartoConfig config = session_.getSessionInfo().getQuartoConfig();
      if (isQuartoSubProject(config)) {
         return false;
      }
      
      
      if (bookType.startsWith("html") || bookType.startsWith("pdf"))
      {
         return true;
      }
      else if (bookType == "all")
      {
         for (int i=0; i<config.project_formats.length; i++) 
         {
            if (config.project_formats[i].startsWith("html"))
               return true;
         }
      }
     
      // fallthrough
      return false;
      
   }
   
   private boolean isQuartoSubProject(QuartoConfig config)
   {
      return !config.project_dir.equals(workbenchContext_.getActiveProjectDir().getPath());
   }
   
   private void quartoServe(String format, boolean render)
   {
      source_.withSaveFilesBeforeCommand(() -> {
         server_.quartoServe(format, render, new SimpleRequestCallback<Void>(constants_.quartoServeError()));
      }, () -> {}, "Quarto");
   }

   private String devtoolsLoadAllPath_ = null;

   private final GlobalDisplay globalDisplay_;
   private final UserPrefs userPrefs_;
   private final BuildServerOperations server_;
   private FilesServerOperations fileServer_;
   private final Display view_;
   private final EventBus eventBus_;
   private final Session session_;
   private final DependencyManager dependencyManager_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Source source_;
   private final WorkbenchContext workbenchContext_;
   private final TerminalHelper terminalHelper_;
   private final Provider<JobManager> pJobManager_;
   private static final ViewBuildtoolsConstants constants_ = GWT.create(ViewBuildtoolsConstants.class);
}
