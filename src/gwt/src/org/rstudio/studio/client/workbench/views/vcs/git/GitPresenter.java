/*
 * GitPresenter.java
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

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.vcs.VCSApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.BaseVcsPresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.VCSFileOpener;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.model.GitHubViewRequest;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

import java.util.ArrayList;

public class GitPresenter extends BaseVcsPresenter implements IsWidget
{
   public interface Binder extends CommandBinder<Commands, GitPresenter> {}

   public interface Display extends WorkbenchView, IsWidget
   {
      void setItems(ArrayList<StatusAndPath> items);
      ArrayList<String> getSelectedPaths();
      ArrayList<StatusAndPath> getSelectedItems();
      int getSelectedItemCount();

      void onRefreshBegin();

      HandlerRegistration addSelectionChangeHandler(
                                          SelectionChangeEvent.Handler handler);

      GitChangelistTable getChangelistTable();
      
      void showContextMenu(int clientX, int clientY);
   }

   @Inject
   public GitPresenter(GitPresenterCore gitCore,
                       VCSFileOpener vcsFileOpener,
                       Display view,
                       GitServerOperations server,
                       final Commands commands,
                       Binder commandBinder,
                       GitState gitState,
                       final GlobalDisplay globalDisplay,
                       SatelliteManager satelliteManager)
   {
      super(view);
      gitPresenterCore_ = gitCore;
      vcsFileOpener_  = vcsFileOpener;
      view_ = view;
      server_ = server;
      commands_ = commands;
      gitState_ = gitState;
      globalDisplay_ = globalDisplay;
      satelliteManager_ = satelliteManager;

      commandBinder.bind(commands, this);

      gitState_.addVcsRefreshHandler(new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view_.setItems(gitState_.getStatus());
         }
      });

      view_.getChangelistTable().addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            int mod = KeyboardShortcut.getModifierValue(event.getNativeEvent());
            if (mod != KeyboardShortcut.NONE)
               return;

            if (event.getNativeKeyCode() == ' ')
            {
               event.preventDefault();
               event.stopPropagation();
               view_.getChangelistTable().toggleStaged(false);
            }
            else if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.preventDefault();
               event.stopPropagation();

               view_.getChangelistTable().toggleStaged(true);
            }
         }
      });
      view_.getChangelistTable().addMouseDownHandler(new MouseDownHandler()
      {
         private DoubleClickState dblClick = new DoubleClickState();
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            if (dblClick.checkForDoubleClick(event.getNativeEvent()))
            {
               event.preventDefault();
               event.stopPropagation();

               view_.getChangelistTable().toggleStaged(false);
            }
         }
      });
      
      view_.getChangelistTable().addContextMenuHandler(new ContextMenuHandler(){
         @Override
         public void onContextMenu(ContextMenuEvent event)
         {
            NativeEvent nativeEvent = event.getNativeEvent();
            view_.showContextMenu(nativeEvent.getClientX(), 
                                  nativeEvent.getClientY());
         }
      });

      view_.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            manageCommands();
         }
      });
      manageCommands();
   }

   private void openSelectedFiles()
   {
      vcsFileOpener_.openFiles(view_.getSelectedItems());
   }

   private void manageCommands()
   {
      boolean anySelected = view_.getSelectedItemCount() > 0;
      commands_.vcsRevert().setEnabled(anySelected);
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   @Handler
   void onVcsDiff()
   {
      showChanges(view_.getSelectedItems());
   }
   
   private void showChanges(ArrayList<StatusAndPath> items)
   {
      showReviewPane(false, null, items);
   }
   
   private void showReviewPane(boolean showHistory, 
                               FileSystemItem historyFileFilter,
                               ArrayList<StatusAndPath> items)
   {
      // setup params
      VCSApplicationParams params = VCSApplicationParams.create(
                                          showHistory, 
                                          historyFileFilter,
                                          items);
      
      // open the window 
      satelliteManager_.openSatellite("review_changes",     
                                      params,
                                      new Size(1000,1200)); 
   }
   
   @Handler
   void onVcsRevert()
   {
      final ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      doRevert(paths, new Command() {
         @Override
         public void execute()
         {
            view_.getChangelistTable().selectNextUnselectedItem();
            view_.getChangelistTable().focus();
         }
         
      });
   }
   
   @Handler
   void onVcsOpen()
   {
      openSelectedFiles();
   }

 
   @Override
   public void onVcsCommit()
   {
      showChanges(view_.getSelectedItems());
   }

   @Override
   public void onVcsShowHistory()
   {
      showHistory(null);
   }
   
   @Override
   public void onVcsPull()
   {
      gitPresenterCore_.onVcsPull();
   }
   
   @Override
   public void onVcsPullRebase()
   {
      gitPresenterCore_.onVcsPullRebase();
   }

   @Override
   public void onVcsPush()
   {
      gitPresenterCore_.onVcsPush();
   }
   
   @Override
   public void onVcsIgnore()
   {
      gitPresenterCore_.onVcsIgnore(view_.getSelectedItems());
   }
   
   @Override
   public void showHistory(FileSystemItem fileFilter)
   {
      showReviewPane(true, fileFilter, new ArrayList<StatusAndPath>());
   }
   
   @Override
   public void showDiff(FileSystemItem file)
   {
      // build an ArrayList<StatusAndPath> so we can call the core helper
      ArrayList<StatusAndPath> diffList = new ArrayList<StatusAndPath>();
      for (StatusAndPath item :  gitState_.getStatus())
      {
         if (item.getRawPath() == file.getPath())
         {
            diffList.add(item);
            break;
         }
      }
      
      if (diffList.size() > 0)
      {
         showChanges(diffList);
      }
      else
      {
         globalDisplay_.showMessage(MessageDialog.INFO,
                                    "No Changes to File", 
                                    "There are no changes to the file \"" + 
                                    file.getName() + "\" to diff.");
      }
      
   }
   
   @Override
   public void revertFile(FileSystemItem file)
   {
      // build an ArrayList<String> so we can call the core helper
      ArrayList<String> revertList = new ArrayList<String>();
      for (StatusAndPath item :  gitState_.getStatus())
      {
         if (item.getRawPath() == file.getPath())
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
            "Changes to the selected " + noun + " will be lost, including " +
            "staged changes.\n\nAre you sure you want to continue?",
            new Operation()
            {
               @Override
               public void execute()
               {
                  if (onRevertConfirmed != null)
                     onRevertConfirmed.execute();
                  
                  server_.gitRevert(
                        revertList,
                        new SimpleRequestCallback<Void>("Revert Changes"));
                  
               }
            },
            false);
   }
   
   @Override
   public void viewOnGitHub(final GitHubViewRequest viewRequest)
   {
      String view = null;
      if (viewRequest.getViewType() == GitHubViewRequest.VCS_VIEW)
         view = "blob";
      else if (viewRequest.getViewType() == GitHubViewRequest.VCS_BLAME)
         view = "blame";
      
      final String path = viewRequest.getFile().getPath();
      server_.gitGithubRemoteUrl(view, 
                                 path, 
                                 new SimpleRequestCallback<String>() {
         
         @Override
         public void onResponseReceived(String url)
         {
            if (url.length() == 0)
            {
               globalDisplay_.showErrorMessage(
                     "Error", 
                     "Unable to view " + path + " on GitHub.\n\n" +
                     "Are you sure that this file is on GitHub and is " +
                     "contained in the currently active project?");
            }
            else
            {
               if (viewRequest.getStartLine() != -1)
                  url += "#L" + viewRequest.getStartLine();
               if (viewRequest.getEndLine() != viewRequest.getStartLine())
                  url += "-L" + viewRequest.getEndLine();
               
               globalDisplay_.openWindow(url);
            }
         }
      });
   }
   
   @Override
   public void onVcsCleanup()
   {
      // svn specific, not supported by git
      
   }

   private final Display view_;
   private final GitPresenterCore gitPresenterCore_;
   private final GitServerOperations server_;
   private final Commands commands_;
   private final GitState gitState_;
   private final GlobalDisplay globalDisplay_;
   private final SatelliteManager satelliteManager_;
   private final VCSFileOpener vcsFileOpener_; 
}
