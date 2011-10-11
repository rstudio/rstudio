/*
 * VCS.java
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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.vcs.events.AskPassEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.frame.VCSPopup;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.model.VcsState;

import java.util.ArrayList;

// TODO: Pull/push results should be shown in a dialog, even on success

public class VCS extends BasePresenter implements IsWidget
{
   public interface Binder extends CommandBinder<Commands, VCS> {}

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
   public VCS(Display view,
              Provider<ReviewPresenter> pReviewPresenter,
              Provider<HistoryPresenter> pHistoryPresenter,
              VCSServerOperations server,
              Commands commands,
              Binder commandBinder,
              VcsState vcsState,
              EventBus events,
              final GlobalDisplay globalDisplay,
              final FileTypeRegistry fileTypeRegistry)
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

      commandBinder.bind(commands, this);

      vcsState_.addVcsRefreshHandler(new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            refresh();
         }
      });

      events.addHandler(AskPassEvent.TYPE, new org.rstudio.studio.client.workbench.views.vcs.events.AskPassEvent.Handler()
      {
         @Override
         public void onAskPass(final AskPassEvent e)
         {
            globalDisplay.promptForPassword(
                  "Password",
                  e.getPrompt(),
                  "",
                  new ProgressOperationWithInput<String>()
                  {
                     @Override
                     public void execute(String input,
                                         final ProgressIndicator indicator)
                     {
                        RSAEncrypt.encrypt_ServerOnly(
                              server_,
                              input,
                              new CommandWithArg<String>()
                              {
                                 @Override
                                 public void execute(String encrypted)
                                 {
                                    server_.askpassCompleted(
                                          encrypted,
                                          new VoidServerRequestCallback(indicator));
                                 }
                              });
                     }
                  },
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        server_.askpassCompleted(
                                           null,
                                           new SimpleRequestCallback<Void>());
                     }
                  });
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
      view_.getChangelistTable().addClickHandler(new ClickHandler()
      {
         private DoubleClickState dblClick = new DoubleClickState();
         @Override
         public void onClick(ClickEvent event)
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
      ReviewPresenter rpres = pReviewPresenter_.get();
      rpres.setSelectedPaths(view_.getSelectedItems());
      VCSPopup.show(rpres,
                    pHistoryPresenter_.get(),
                    showHistory);
   }

   @Handler
   void onVcsStage()
   {
      ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      server_.vcsAdd(paths, new SimpleRequestCallback<Void>("Stage Changes"));
   }

   @Handler
   void onVcsUnstage()
   {
      ArrayList<String> paths = view_.getSelectedPaths();
      if (paths.size() == 0)
         return;

      server_.vcsUnstage(paths,
                         new SimpleRequestCallback<Void>("Unstage Changes"));
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
   void onVcsRefresh()
   {
      view_.onRefreshBegin();
      vcsState_.refresh();
   }

   @Handler
   void onVcsShowHistory()
   {
      showReviewPane(true);
   }

   @Handler
   void onVcsPull()
   {
      server_.vcsPull(new SimpleRequestCallback<ConsoleProcess>() {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog("Pull", proc).showModal();
         }
      });
   }

   @Handler
   void onVcsPush()
   {
      server_.vcsPush(new SimpleRequestCallback<ConsoleProcess>() {
         @Override
         public void onResponseReceived(ConsoleProcess proc)
         {
            new ConsoleProgressDialog("Push", proc).showModal();
         }
      });
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
}
