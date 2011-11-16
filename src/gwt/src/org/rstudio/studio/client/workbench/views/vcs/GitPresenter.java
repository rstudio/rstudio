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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;

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
import org.rstudio.studio.client.common.satellite.SatelliteWindowPrefs;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.frame.VCSPopup;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
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

      ChangelistTable getChangelistTable();
   }

   @Inject
   public GitPresenter(GitPresenterCore gitCore,
                       Display view,
                       Provider<ReviewPresenter> pReviewPresenter,
                       Provider<HistoryPresenter> pHistoryPresenter,
                       VCSServerOperations server,
                       final Commands commands,
                       Binder commandBinder,
                       VcsState vcsState,
                       EventBus events,
                       final GlobalDisplay globalDisplay,
                       final FileTypeRegistry fileTypeRegistry,
                       SatelliteManager satelliteManager,
                       Provider<UIPrefs> pUIPrefs)
   {
      super(view);
      view_ = view;
      pReviewPresenter_ = pReviewPresenter;
      pHistoryPresenter_ = pHistoryPresenter;
      server_ = server;
      commands_ = commands;
      vcsState_ = vcsState;
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      satelliteManager_ = satelliteManager;
      pUIPrefs_ = pUIPrefs;

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
         fileTypeRegistry_.openFile(FileSystemItem.createFile(
               item.getRawPath()));
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
      // show in external window if we shift-click
      Event currentEvent = Event.getCurrentEvent();
      if ((currentEvent != null) && currentEvent.getShiftKey())
      {
         satelliteManager_.openSatellite("review_changes", 
                                         getPreferredReviewPanelSize());
      }
      else
      {
         ReviewPresenter rpres = pReviewPresenter_.get();
         if (view_.getSelectedItemCount() > 0)
            rpres.setSelectedPaths(view_.getSelectedItems());
         VCSPopup.show(rpres,
                       pHistoryPresenter_.get(),
                       showHistory);
      }
   }
   
   private Size getPreferredReviewPanelSize()
   { 
      // get global window bounds
      Size windowBounds = new Size(Window.getClientWidth(),
                                   Window.getClientHeight());
      
      // if we have saved prefs then use them (but still enforce boundaries)
      UIPrefs uiPrefs = pUIPrefs_.get();
      SatelliteWindowPrefs vcsPrefs = uiPrefs.vcsWindowPrefs().getValue();
      if (!vcsPrefs.isEmpty())
      {
         // get prefs size (and defend against perversely small values)
         Size prefsSize = new Size(vcsPrefs.getWidth(), vcsPrefs.getHeight());
         prefsSize = new Size(Math.max(500,prefsSize.width),
                              Math.max(500, prefsSize.height));
         
         // return prefs size (bounded by main window size)
         return new Size(
               Math.min(windowBounds.width - 25, vcsPrefs.getWidth()),
               Math.min(windowBounds.height - 25, vcsPrefs.getHeight()));
         
      }
      else
      {
         // default to 1000 pixels wide (capped by window width) and
         // the full height of the window
         return new Size(Math.min(windowBounds.width - 25, 1000), 
                         windowBounds.height - 50);
      }
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

                  server_.vcsRevert(
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
      JsArray<StatusAndPath> status = vcsState_.getStatus();
      ArrayList<StatusAndPath> list = new ArrayList<StatusAndPath>();
      for (int i = 0; i < status.length(); i++)
         list.add(status.get(i));
      view_.setItems(list);
   }

   private final Display view_;
   private final Provider<ReviewPresenter> pReviewPresenter_;
   private final Provider<HistoryPresenter> pHistoryPresenter_;
   private final VCSServerOperations server_;
   private final Commands commands_;
   private final VcsState vcsState_;
   private final GlobalDisplay globalDisplay_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final SatelliteManager satelliteManager_;
   private final Provider<UIPrefs> pUIPrefs_;
}
