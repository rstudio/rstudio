/*
 * GitPresenterCore.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.vcs.git;


import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.ProcessResult;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.ignore.Ignore;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

@Singleton
public class GitPresenterCore
{
   public interface Binder extends CommandBinder<Commands, GitPresenterCore> {}
   
   @Inject
   public GitPresenterCore(GitServerOperations server,
                           GitState gitState,
                           Provider<Ignore> pIgnore,
                           final Commands commands,
                           Binder commandBinder,
                           EventBus eventBus,
                           final GlobalDisplay globalDisplay,
                           final Satellite satellite,
                           final SatelliteManager satelliteManager)
   {
      server_ = server;
      gitState_ = gitState;
      pIgnore_ = pIgnore;
      
      commandBinder.bind(commands, this);

      gitState_.addVcsRefreshHandler(new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            boolean hasRemote = gitState_.hasRemote();
            commands.vcsPull().setEnabled(hasRemote);
            commands.vcsPush().setEnabled(hasRemote);
         }
      });
   }

   @Handler
   void onVcsRefresh()
   {
      gitState_.refresh();
   }

   @Handler
   void onVcsRefreshNoError()
   {
      gitState_.refresh(false);
   }


   public void onVcsPull()
   {
      server_.gitPull(new SimpleRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog(proc, server_).showModal();
         }
      });
   }
   
   public void onVcsPullRebase()
   {
      server_.gitPullRebase(new SimpleRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog(proc, server_).showModal();
         }
      });
   }

   public void onVcsPush()
   {
      server_.gitPush(new SimpleRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog(proc, server_).showModal();
         }
      });
   }
   
   public void onVcsIgnore(ArrayList<StatusAndPath> items)
   {
      ArrayList<String> paths = getPathArray(items);
     
      pIgnore_.get().showDialog(paths, new Ignore.Strategy() {

         @Override
         public String getDialogCaption()
         {
            return "Git Ignore";
         }
         
         @Override
         public String getIgnoresCaption()
         {
            return ".gitignore";
         }
         
         @Override
         public String getHelpLinkName()
         {
            return "git_ignore_help";
         }
         
         @Override
         public Ignore.Strategy.Filter getFilter()
         {
            return new Ignore.Strategy.Filter() {
               @Override
               public boolean includeFile(FileSystemItem file)
               {
                  return file.getName() != ".gitignore";
               }
            };
         }

         @Override
         public void getIgnores(String path,
               ServerRequestCallback<ProcessResult> requestCallback)
         {
            server_.gitGetIgnores(path, requestCallback);
         }

         @Override
         public void setIgnores(String path, String ignores,
               ServerRequestCallback<ProcessResult> requestCallback)
         {
            server_.gitSetIgnores(path, ignores, requestCallback);
         }
      });
      
   }
   
   private ArrayList<String> getPathArray(ArrayList<StatusAndPath> items)
   {
      ArrayList<String> paths = new ArrayList<String>();
      for (StatusAndPath item : items)
         paths.add(item.getPath());
      return paths;
   }
    
   private final GitServerOperations server_;
   private final GitState gitState_;
   private final Provider<Ignore> pIgnore_;
}
