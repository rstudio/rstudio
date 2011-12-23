/*
 * SVNCommandHandler.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.svn;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.vcs.IgnoreDialog;
import org.rstudio.studio.client.common.vcs.IgnoreStrategy;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.SVNServerOperations.ProcessResult;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.ProcessCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.VCSFileOpener;
import org.rstudio.studio.client.workbench.views.vcs.svn.commit.SVNCommitDialog;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;

public class SVNCommandHandler
{
   public interface Binder extends CommandBinder<Commands, SVNCommandHandler>
   {
   }

   public SVNCommandHandler(SVNPresenterDisplay display,
                            GlobalDisplay globalDisplay,
                            Commands commands,
                            SVNServerOperations server,
                            SVNState svnState,
                            VCSFileOpener vcsFileOpener)
   {
      display_ = display;
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      server_ = server;
      svnState_ = svnState;
      vcsFileOpener_ = vcsFileOpener;
      GWT.<Binder>create(Binder.class).bind(commands, this);

      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(Provider<SVNCommitDialog> pCommitDialog)
   {
      pCommitDialog_ = pCommitDialog;
   }
   
   public void setFilesCommandsEnabled(boolean enabled)
   {
      commands_.vcsAddFiles().setEnabled(enabled);
      commands_.vcsRemoveFiles().setEnabled(enabled);
      commands_.vcsRevert().setEnabled(enabled);
      commands_.vcsIgnore().setEnabled(enabled);
   }
   
   // onVcsPull and onVcsCommit and not direct  command handlers because they 
   // are handled globally in the main frame (by BaseVcsPresenter or the 
   // satellite frame)
   
   public void onVcsPull()
   {
      server_.svnUpdate(new SimpleRequestCallback<ConsoleProcess>()
         {
            @Override
            public void onResponseReceived(ConsoleProcess response)
            {
               new ConsoleProgressDialog(response, server_).showModal();
            }
         });
   }
   
   public void onVcsCommit()
   {
      pCommitDialog_.get().showModal();
   }
   
   @Handler
   void onVcsIgnore()
   {
      ArrayList<String> paths = getPathArray();

      if (paths.size() > 0)
      {
         IgnoreDialog.show(paths, new IgnoreStrategy() {

            @Override
            public String getCaption()
            {
               return "SVN Ignore";
            }

            @Override
            public void getIgnores(String path,
                  ServerRequestCallback<ProcessResult> requestCallback)
            {
               server_.svnGetIgnores(path, requestCallback);
            }

            @Override
            public void setIgnores(String path, String ignores,
                  ServerRequestCallback<ProcessResult> requestCallback)
            {
               server_.svnSetIgnores(path, ignores, requestCallback);
            }
            
         });
      }
   }
   
   @Handler
   void onVcsOpen()
   {
      vcsFileOpener_.openFiles(display_.getSelectedItems());
   }
   
   @Handler
   void onVcsRefresh()
   {
      display_.getChangelistTable().showProgress();
      svnState_.refresh(true);
   }
   
   @Handler
   void onVcsAddFiles()
   {
      ArrayList<String> paths = getPathArray();

      if (paths.size() > 0)
         server_.svnAdd(paths, new ProcessCallback("SVN Add"));
   }
   
   @Handler
   void onVcsRemoveFiles()
   {
      ArrayList<String> paths = getPathArray();

      if (paths.size() > 0)
         server_.svnDelete(paths, new ProcessCallback("SVN Delete"));
   }
   
   @Handler
   void onVcsRevert()
   {
      final ArrayList<String> paths = getPathArray();
      if (paths.size() == 0)
         return;

      doRevert(paths, new Command() {
         @Override
         public void execute()
         {
            display_.getChangelistTable().selectNextUnselectedItem();
            display_.getChangelistTable().focus();
         }
         
      });
   }
   
   public void revertFile(FileSystemItem file)
   {
      // build an ArrayList<String> so we can call the core helper
      ArrayList<String> revertList = new ArrayList<String>();
      for (StatusAndPath item :  svnState_.getStatus())
      {
         if (item.getRawPath().equals(file.getPath()))
         {
            revertList.add(item.getPath());
            break;
         }
      }
      
      if (revertList.size() > 0)
      {
         doRevert(revertList, null);
      }
      else
      {
         globalDisplay_.showMessage(MessageDialog.INFO,
                                    "No Changes to Revert", 
                                    "There are no changes to the file \"" + 
                                    file.getName() + "\" to revert.");
      }
      
   }
    
   private void doRevert(final ArrayList<String> revertList, 
                         final Command onRevertConfirmed)
   {
      String noun = revertList.size() == 1 ? "file" : "files";
      globalDisplay_.showYesNoMessage(
            GlobalDisplay.MSG_WARNING,
            "Revert Changes",
            "Changes to the selected " + noun + " will be reverted.\n\n" +
                  "Are you sure you want to continue?",
                  new Operation()
            {
               @Override
               public void execute()
               {
                  if (onRevertConfirmed != null)
                     onRevertConfirmed.execute();

                  server_.svnRevert(revertList, 
                                    new ProcessCallback("SVN Revert"));

               }
            },
            false);
   }
   
   
   private ArrayList<String> getPathArray()
   {
      ArrayList<StatusAndPath> items = display_.getSelectedItems();
      ArrayList<String> paths = new ArrayList<String>();
      for (StatusAndPath item : items)
         paths.add(item.getPath());
      return paths;
   }
   
   private final SVNPresenterDisplay display_;
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final SVNServerOperations server_;
   private final SVNState svnState_;
   private final VCSFileOpener vcsFileOpener_;
   private Provider<SVNCommitDialog> pCommitDialog_;
}
