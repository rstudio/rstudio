/*
 * ProfilerPresenter.java
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
package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationRequest;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationResponse;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerContents;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ProfilerPresenter implements RprofEvent.Handler
{
   private final ProfilerServerOperations server_;
   private final SourceServerOperations sourceServer_;
   private final Commands commands_;
   private final DependencyManager dependencyManager_;
   private final HandlerRegistrations handlerRegistrations_ = new HandlerRegistrations();
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   private Provider<SourceWindowManager> pSourceWindowManager_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileContext_;
   private final WorkbenchContext workbenchContext_;
   private final FileTypeRegistry fileTypeRegistry_;
   private String currentDocId_;
   
   final String profilerDependecyUserAction_ = "Preparing profiler";
   
   private ProfileOperationResponse response_ = null;

   public interface Binder extends CommandBinder<Commands, ProfilerPresenter>
   {
   }
   
   public interface Display
   {
   }

   @Inject
   public ProfilerPresenter(ProfilerServerOperations server,
                            Binder binder,
                            Commands commands,
                            DependencyManager dependencyManager,
                            GlobalDisplay globalDisplay,
                            EventBus events,
                            Provider<SourceWindowManager> pSourceWindowManager,
                            FileDialogs fileDialogs,
                            RemoteFileSystemContext fileContext,
                            WorkbenchContext workbenchContext,
                            FileTypeRegistry fileTypeRegistry,
                            SourceServerOperations sourceServer)
   {
      server_ = server;
      commands_ = commands;
      dependencyManager_ = dependencyManager;
      globalDisplay_ = globalDisplay;
      events_ = events;
      pSourceWindowManager_ = pSourceWindowManager;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      workbenchContext_ = workbenchContext;
      fileTypeRegistry_ = fileTypeRegistry;
      sourceServer_ = sourceServer;
      
      binder.bind(commands, this);

      // by default, one can always start profiling
      enableStoppedCommands();
      
      events_.addHandler(RprofEvent.TYPE, this);
   }
   
   public void onRprofEvent(RprofEvent event)
   {
      switch (event.getEventType())
      {
         case START:
            enableStartedCommands();
            break;
         case STOP:
            enableStoppedCommands();
            break;
         case CREATE:
            events_.fireEvent(new OpenProfileEvent(
               event.getData().getPath(),
               event.getData().getHtmlPath(),
               event.getData().getHtmlLocalPath(),
               true,
               currentDocId_));
            currentDocId_ = null;
            break;
         default:
            break;
      }
   }

   public void attach(SourceDocument doc, Display view)
   {
   }

   public void detach()
   {
      // unsubscribe from view
      handlerRegistrations_.removeHandler();
   }

   @Handler
   public void onStartProfiler()
   {
      dependencyManager_.withProfvis(profilerDependecyUserAction_, new Command()
      {
         @Override
         public void execute()
         {
            ProfileOperationRequest request = ProfileOperationRequest
                  .create("");
            server_.startProfiling(request,
                  new ServerRequestCallback<ProfileOperationResponse>()
            {
               @Override
               public void onResponseReceived(ProfileOperationResponse response)
               {
                  if (response.getErrorMessage() != null)
                  {
                     globalDisplay_.showErrorMessage("Profiler Error",
                           response.getErrorMessage());
                     return;
                  }
                  
                  pSourceWindowManager_.get().ensureVisibleSourcePaneIfNecessary();

                  response_ = response;
                  
                  sourceServer_.newDocument(
                     FileTypeRegistry.PROFILER.getTypeId(),
                     null,
                     (JsObject) ProfilerContents.create(
                           response.getFileName(),
                           null,
                           null,
                           true).cast(),
                     new SimpleRequestCallback<SourceDocument>("Show Profiler")
                     {
                        @Override
                        public void onResponseReceived(SourceDocument response)
                        {
                           currentDocId_ = response.getId();
                        }
                        
                        @Override
                        public void onError(ServerError error)
                        {
                           Debug.logError(error);
                        }
                     });
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  globalDisplay_.showErrorMessage("Failed to Stop Profiler",
                        error.getMessage());
               }
            });
         }
      });
   }

   @Handler
   public void onStopProfiler()
   {
      ProfileOperationRequest request = ProfileOperationRequest
            .create(response_ != null ? response_.getFileName() : null);

      response_ = null;
      server_.stopProfiling(request,
            new ServerRequestCallback<ProfileOperationResponse>()
            {
               @Override
               public void onResponseReceived(ProfileOperationResponse response)
               {
                  if (response.getErrorMessage() != null)
                  {
                     globalDisplay_.showErrorMessage("Profiler Error",
                           response.getErrorMessage());
                     return;
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  globalDisplay_.showErrorMessage("Failed to Stop Profiler",
                        error.getMessage());
               }
            });
   }
   
   @Handler
   public void onOpenProfile()
   {
      fileDialogs_.openFile(
         "Open File",
         fileContext_,
         workbenchContext_.getDefaultFileDialogDir(),
         "Profvis Profiles (*.Rprofvis)",
         new ProgressOperationWithInput<FileSystemItem>()
         {
            public void execute(final FileSystemItem input,
                                ProgressIndicator indicator)
            {
               if (input == null)
                  return;
   
               workbenchContext_.setDefaultFileDialogDir(
                                                input.getParentPath());
   
               indicator.onCompleted();
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  public void execute()
                  {
                     fileTypeRegistry_.openFile(input);
                  }
               });
            }
         });
   }
   
   @Handler
   public void onSaveProfileAs()
   {
      commands_.saveSourceDocAs().execute();
   }
   
   public void buildHtmlPath(final OperationWithInput<ProfileOperationResponse> continuation,
                             final Operation onError,
                             final String path)
   {
      ProfileOperationRequest request = ProfileOperationRequest.create(path);
       
      server_.openProfile(request, 
            new ServerRequestCallback<ProfileOperationResponse>()
      {
         @Override
         public void onResponseReceived(ProfileOperationResponse response)
         {
            if (response.getErrorMessage() != null)
            {
               globalDisplay_.showErrorMessage("Profiler Error",
                     response.getErrorMessage());
               onError.execute();
               return;
            }
             
            continuation.execute(response);
         }
   
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            globalDisplay_.showErrorMessage("Failed to Open Profile",
                  error.getMessage());
            onError.execute();
         }
      });
   }

   private void enableStartedCommands()
   {
      commands_.startProfiler().setEnabled(false);
      commands_.stopProfiler().setEnabled(true);
   }

   private void enableStoppedCommands()
   {
      commands_.startProfiler().setEnabled(true);
      commands_.stopProfiler().setEnabled(false);
   }
}
