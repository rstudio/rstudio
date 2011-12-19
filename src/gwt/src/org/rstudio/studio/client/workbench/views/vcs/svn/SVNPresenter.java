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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
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
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;

public class SVNPresenter extends BaseVcsPresenter
{
   public interface Binder extends CommandBinder<Commands, SVNPresenter>
   {
   }

   public interface Display extends WorkbenchView, IsWidget
   {
      HasClickHandlers getDiffButton();
      HasClickHandlers getAddFilesButton();
      HasClickHandlers getDeleteFilesButton();
      HasClickHandlers getRevertFilesButton();
      HasClickHandlers getUpdateButton();
      HasClickHandlers getCommitButton();
      ArrayList<StatusAndPath> getSelectedItems();
      ChangelistTable getChangelistTable();
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
      globalDisplay_ = globalDisplay;
      server_ = server;
      svnState_ = svnState;
      satelliteManager_ = satelliteManager;
      vcsFileOpener_ = vcsFileOpener;
      
      binder.bind(commands, this);

      view_.getDiffButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onVcsDiff();
         }
      });

      view_.getAddFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ArrayList<String> paths = getPathArray();

            if (paths.size() > 0)
               server_.svnAdd(paths, new ProcessCallback("SVN Add"));
         }
      });

      view_.getDeleteFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ArrayList<String> paths = getPathArray();

            if (paths.size() > 0)
               server_.svnDelete(paths, new ProcessCallback("SVN Delete"));
         }
      });

      view_.getRevertFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onVcsRevert();
         }
      });

      view_.getUpdateButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onVcsPull();
         }
      });

      view_.getCommitButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            onVcsCommit();
         }
      });
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
            "Changes to the selected " + noun + " will be lost, including " +
                  "staged changes.\n\nAre you sure you want to continue?",
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

   @Handler
   void onVcsRefresh()
   {
      svnState_.refresh(true);
   }
   
   @Override
   public void onVcsPush()
   {
      // git specific,  not supported by svn
   }
   

   private final Display view_;
   private final GlobalDisplay globalDisplay_;
   private final SVNServerOperations server_;
   private final SVNState svnState_;
   private final SatelliteManager satelliteManager_;
   private final VCSFileOpener vcsFileOpener_;
   
   
}
