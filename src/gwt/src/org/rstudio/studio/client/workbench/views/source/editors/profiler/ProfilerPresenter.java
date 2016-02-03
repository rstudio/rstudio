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
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfileOperationResponse;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProfilerPresenter
{ 
   public interface Display
   {
   }

   @Inject
   public ProfilerPresenter(ProfilerServerOperations server,
                            Binder binder,
                            Commands commands,
                            DependencyManager dependencyManager)
   {
      server_ = server;
      commands_ = commands;
      dependencyManager_ = dependencyManager;
      
      binder.bind(commands, this);
      
      // by default, one can always start profiling
      enableStoppedCommands();
   }
   
   public void attatch(SourceDocument doc, Display view)
   {
      // enable commands for stopped state
      enableStoppedCommands();
   }
   
   public void detach()
   {
      // unsubscribe from view
      handlerRegistrations_.removeHandler();
      
      // disable all commands
      disableAllCommands();
   }
   
   @Handler
   public void onStartProfiler()
   {
      dependencyManager_.withProfvis(profilerDependecyUserAction_, new Command()
      {
         @Override
         public void execute()
         {
            // manage commands
            enableStartedCommands();
            
            server_.startProfiling(new ServerRequestCallback<ProfileOperationResponse>()
            {
               @Override
               public void onError(ServerError error)
               {
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
      
      server_.stopProfiling(new ServerRequestCallback<ProfileOperationResponse>()
      {
         @Override
         public void onError(ServerError error)
         {
         }
      });
   }
   
   private void disableAllCommands()
   {
      commands_.startProfiler().setEnabled(false);
      commands_.stopProfiler().setEnabled(false);
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
   
   private final ProfilerServerOperations server_;
   private final Commands commands_;
   private final DependencyManager dependencyManager_;
   private final HandlerRegistrations handlerRegistrations_ = 
                                             new HandlerRegistrations();
   final String profilerDependecyUserAction_ = "Preparing profiler";
   
   public interface Binder extends CommandBinder<Commands, ProfilerPresenter> {}
}
