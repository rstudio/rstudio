/*
 * SVNPresenter.java
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

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.vcs.VCSApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.vcs.BaseVcsPresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.common.ProcessCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.VCSFileOpener;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;

public class SVNPresenter extends BaseVcsPresenter
{
   public interface Binder extends CommandBinder<Commands, SVNPresenter>
   {
   }

   public interface Display extends WorkbenchView, IsWidget
   {   
      void setItems(ArrayList<StatusAndPath> items);
      
      ArrayList<StatusAndPath> getSelectedItems();
      int getSelectedItemCount();
      
      HandlerRegistration addSelectionChangeHandler(
                                    SelectionChangeEvent.Handler handler);
      
      ChangelistTable getChangelistTable();
      
      void showContextMenu(int clientX, int clientY);
   }

   @Inject
   public SVNPresenter(Display view,
                       GlobalDisplay globalDisplay,
                       Binder binder,
                       Commands commands,
                       SVNServerOperations server,
                       SVNState svnState,
                       SatelliteManager satelliteManager,
                       VCSFileOpener vcsFileOpener)
   {
      super(view);
      view_ = view;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      server_ = server;
      svnState_ = svnState;
      satelliteManager_ = satelliteManager;
      vcsFileOpener_ = vcsFileOpener;
      
      binder.bind(commands, this);
      
      svnState_.addVcsRefreshHandler(new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            view_.setItems(svnState_.getStatus());
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
                                      getPreferredReviewPanelSize()); 
   }

   private Size getPreferredReviewPanelSize()
   {
      Size windowBounds = new Size(Window.getClientWidth(),
                                   Window.getClientHeight());

      return new Size(Math.min(windowBounds.width - 100, 1000),
                      windowBounds.height - 25);
   }

   private ArrayList<String> getPathArray()
   {
      ArrayList<StatusAndPath> items = view_.getSelectedItems();
      ArrayList<String> paths = new ArrayList<String>();
      for (StatusAndPath item : items)
         paths.add(item.getPath());
      return paths;
   }
   
   private void openSelectedFiles()
   {
      vcsFileOpener_.openFiles(view_.getSelectedItems());
   }
   
   private void manageCommands()
   {
      boolean anySelected = view_.getSelectedItemCount() > 0;
      
      
      
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
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
   void onVcsDiff()
   {
      showChanges(view_.getSelectedItems());
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
   
   
   @Handler
   void onVcsRefresh()
   {
      svnState_.refresh(true);
   }
   
  
   // the following commands are BaseVcsPresenter overrides rather than
   // direct handlers (they are handled within the base class and then
   // dispatched to via these overrides)
   
   @Override
   public void onVcsCommit()
   {
      
      
   }

   @Override
   public void onVcsShowHistory()
   {
      showHistory(null);
   }

   @Override
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
      for (StatusAndPath item :  svnState_.getStatus())
      {
         if (item.getRawPath().equals(file.getPath()))
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

   @Override
   public void onVcsPush()
   {
      // git specific,  not supported by svn
   }
   

   private final Display view_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final SVNServerOperations server_;
   private final SVNState svnState_;
   private final SatelliteManager satelliteManager_;
   private final VCSFileOpener vcsFileOpener_;
   
   
}
