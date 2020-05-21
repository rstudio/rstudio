/*
 * SVNCommitDialog.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn.commit;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.console.ProcessExitEvent.Handler;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNSelectChangelistTablePresenter;

public class SVNCommitDialog extends ModalDialogBase
{
   interface Binder extends UiBinder<Widget, SVNCommitDialog>
   {
   }

   @Inject
   public SVNCommitDialog(SVNServerOperations server,
                          final SVNSelectChangelistTablePresenter changelistPresenter,
                          GlobalDisplay globalDisplay,
                          Session session)
   {
      super(Roles.getDialogRole());
      server_ = server;
      globalDisplay_ = globalDisplay;
      session_ = session;

      setText("Commit");

      if (commitDraftStateValue_ == null)
      {
         commitDraftStateValue_ = new StringStateValue(
               MODULE_SVN,
               KEY_COMMIT_MESSAGE,
               ClientState.PROJECT_PERSISTENT,
               session.getSessionInfo().getClientState())
         {
            @Override
            protected void onInit(String value)
            {
               commitDraft_ = value;
            }

            @Override
            protected String getValue()
            {
               return commitDraft_;
            }
         };
      }

      ThemedButton commitButton = new ThemedButton("Commit",
                                                    new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            attemptCommit();
         }
      });
      addButton(commitButton, ElementIds.DIALOG_OK_BUTTON);
      addCancelButton();
      
     
      changelist_ = changelistPresenter.getView();
      widget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      widget_.setSize("500px", "400px");

      topHPanel_.setCellWidth(selectLabel_, "99%");
      topHPanel_.setCellVerticalAlignment(selectLabel_,
                                          HorizontalPanel.ALIGN_BOTTOM);

      topHPanel_.setCellWidth(btnClearSelection_, "1%");
      topHPanel_.setCellVerticalAlignment(btnClearSelection_,
                                          HorizontalPanel.ALIGN_TOP);
      topHPanel_.setCellHorizontalAlignment(btnClearSelection_,
                                            HorizontalPanel.ALIGN_RIGHT);

      lblMessage_.setFor(message_);

      btnClearSelection_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            changelistPresenter.clearSelection();
         }
      });

      DomUtils.disableSpellcheck(message_);
      
      if (!StringUtil.isNullOrEmpty(commitDraft_))
         message_.setText(commitDraft_);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            message_.setFocus(true);
         }
      });
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      commitDraft_ = message_.getText();
      session_.persistClientState();
   }
 
   private void attemptCommit()
   {
      if (validateInput())
      {
         server_.svnCommit(
               changelist_.getSelectedPaths(),
               message_.getText(),
               new SimpleRequestCallback<ConsoleProcess>("SVN Commit")
               {
                  private int exitCode_ = 0;
                  
                  @Override
                  public void onResponseReceived(ConsoleProcess cp)
                  {
                     // hide the dialog -- we'll re-show it if the commit fails
                     SVNCommitDialog.this.setVisible(false);
                     
                     // subscribe to process exit so we can record the
                     // exit code and manage the commit draft persistence
                     cp.addProcessExitHandler(new Handler()
                     {
                        @Override
                        public void onProcessExit(ProcessExitEvent event)
                        {
                           // save the exit code so we can use it to decide
                           // whether to become visible or close the dialog
                           // once the console process dialog exits
                           exitCode_ = event.getExitCode();
                           
                           // We'll set the commitDraft_ on unload, so clear
                           // out the text box now
                           if (exitCode_  == 0)
                              message_.setText("");
                        }
                     });

                     // create the console progress dialog and then subscribe
                     // to its onClose event to figure out whether we need
                     // to re-appear or fully close
                     ConsoleProgressDialog dialog = new ConsoleProgressDialog(
                           cp,
                           server_);
                     dialog.addCloseHandler(new CloseHandler<PopupPanel>() {
                        @Override
                        public void onClose(CloseEvent<PopupPanel> event)
                        {
                           if (exitCode_== 0)
                              closeDialog();
                           else
                              SVNCommitDialog.this.setVisible(true);
                        }
                     });
                 
                     // show the dialog
                     dialog.showModal();
                  }
               });
      }
   }

   private boolean validateInput()
   {
      if (changelist_.getSelectedPaths().size() == 0)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_WARNING,
                                    "No Items Selected",
                                    "Please select one or more items to " +
                                    "commit.",
                                    message_);
         return false;
      }

      // actually validate
      if (message_.getText().trim().length() == 0)
      {
         globalDisplay_.showMessage(GlobalDisplay.MSG_WARNING,
                                    "Message Required",
                                    "Please provide a commit message.",
                                    message_);
         return false;
      }


      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }

   @UiField(provided = true)
   ChangelistTable changelist_;
   @UiField
   FormLabel lblMessage_;
   @UiField
   TextArea message_;
   @UiField
   HorizontalPanel topHPanel_;
   @UiField
   SmallButton btnClearSelection_;
   @UiField
   Label selectLabel_;

   private Widget widget_;
   private final SVNServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;

   private static String commitDraft_;
   private static StringStateValue commitDraftStateValue_;

   private static final String MODULE_SVN = "svn";
   private static final String KEY_COMMIT_MESSAGE = "commitMessage";
}
