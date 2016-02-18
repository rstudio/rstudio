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

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationRequest;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationResponse;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProfilerPresenter implements RprofEvent.Handler
{
   private final ProfilerServerOperations server_;
   private final Commands commands_;
   private final DependencyManager dependencyManager_;
   private final HandlerRegistrations handlerRegistrations_ = new HandlerRegistrations();
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   
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
                            EventBus events)
   {
      server_ = server;
      commands_ = commands;
      dependencyManager_ = dependencyManager;
      globalDisplay_ = globalDisplay;
      events_ = events;
      
      binder.bind(commands, this);

      // by default, one can always start profiling
      enableStoppedCommands();
      
      events_.addHandler(RprofEvent.TYPE, this);
   }
   
   public void onRprofEvent(RprofEvent event)
   {
      if (event.getStarted())
      {
         enableStartedCommands();
      }
      else
      {
         enableStoppedCommands();
      }
   }

   public void attatch(SourceDocument doc, Display view)
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

                  response_ = response;
               }

               @Override
               public void onError(ServerError error)
               {
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
      // manage commands
      enableStoppedCommands();

      ProfileOperationRequest request = ProfileOperationRequest
            .create(response_.getFileName());

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
                  globalDisplay_.showErrorMessage("Failed to Stop Profiler",
                        error.getMessage());
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
