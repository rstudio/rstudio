/*
 * GitPresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
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
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.vcs.VCSApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.files.events.DirectoryNavigateEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.VcsState;

import java.util.ArrayList;

public class GitPresenter extends BasePresenter implements IsWidget
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
   }

   @Inject
   public GitPresenter(GitPresenterCore gitCore,
                       Display view,
                       GitServerOperations server,
                       final Commands commands,
                       Binder commandBinder,
                       VcsState vcsState,
                       EventBus events,
                       final GlobalDisplay globalDisplay,
                       final FileTypeRegistry fileTypeRegistry,
                       SatelliteManager satelliteManager)
   {
      super(view);
      view_ = view;
      server_ = server;
      commands_ = commands;
      eventBus_ = events;
      vcsState_ = vcsState;
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      satelliteManager_ = satelliteManager;

      commandBinder.bind(commands, this);

      vcsState_.addVcsRefreshHandler(new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            refresh();
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

               openSelectedFile();
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

               openSelectedFile();
            }
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

   private void openSelectedFile()
   {
      if (view_.getSelectedItemCount() == 0)
         return;

      ArrayList<StatusAndPath> items = view_.getSelectedItems();
      for (StatusAndPath item : items)
      {
         if (!item.isDirectory())
         {
            fileTypeRegistry_.openFile(
                           FileSystemItem.createFile(item.getRawPath()));
         }
         else 
         { 
            eventBus_.fireEvent(new DirectoryNavigateEvent(
                           FileSystemItem.createDir(item.getRawPath())));
            commands_.activateFiles().execute();
         }
      }
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
      showReviewPane(false);
   }
   
  
   private void showReviewPane(boolean showHistory)
   {
      // setup params
      VCSApplicationParams params = VCSApplicationParams.create(
                                          showHistory, 
                                          view_.getSelectedItems());
      
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

   @Handler
   void onVcsRevert()
   {
      final ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      String noun = paths.size() == 1 ? "file" : "files";
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
                  view_.getChangelistTable().selectNextUnselectedItem();

                  server_.gitRevert(
                        paths,
                        new SimpleRequestCallback<Void>("Revert Changes"));
               }
            },
            false);
   }

   @Handler
   void onVcsCommit()
   {
      showReviewPane(false);
   }

   @Handler
   void onVcsShowHistory()
   {
      showReviewPane(true);
   }

   private void refresh()
   {
      view_.setItems(vcsState_.getStatus());
   }

   private final Display view_;
   private final GitServerOperations server_;
   private final Commands commands_;
   private final VcsState vcsState_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final SatelliteManager satelliteManager_;
}
