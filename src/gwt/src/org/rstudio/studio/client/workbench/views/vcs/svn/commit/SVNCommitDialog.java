/*
 * SVNCommitDialog.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn.commit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.console.ProcessExitEvent.Handler;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNSelectChangelistTablePresenter;

public class SVNCommitDialog extends ModalDialog<Void>
{
   interface Binder extends UiBinder<Widget, SVNCommitDialog>
   {
   }

   @Inject
   public SVNCommitDialog(SVNServerOperations server,
                          final SVNSelectChangelistTablePresenter changelistPresenter,
                          GlobalDisplay globalDisplay)
   {
      super("Commit", new OperationWithInput<Void>()
      {
         @Override
         public void execute(Void input)
         {
         }
      });
      server_ = server;
      globalDisplay_ = globalDisplay;

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

      btnClearSelection_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            changelistPresenter.clearSelection();
         }
      });

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
      commitDraft_ = message_.getText();
   }

   @Override
   protected Void collectInput()
   {
      return null;
   }

   @Override
   protected void validateAndGo(Void input, final Command executeOnSuccess)
   {
      if (validate(input))
      {
         server_.svnCommit(
               changelist_.getSelectedPaths(),
               message_.getText(),
               new SimpleRequestCallback<ConsoleProcess>("SVN Commit")
               {
                  @Override
                  public void onResponseReceived(ConsoleProcess cp)
                  {
                     cp.addProcessExitHandler(new Handler()
                     {
                        @Override
                        public void onProcessExit(ProcessExitEvent event)
                        {
                           if (event.getExitCode() == 0)
                           {
                              // We'll set the commitDraft_ on unload, so clear
                              // out the text box now
                              message_.setText("");

                              executeOnSuccess.execute();
                           }
                        }
                     });

                     ConsoleProgressDialog dialog = new ConsoleProgressDialog(
                           cp,
                           server_);
                     dialog.showModal();
                  }
               });
      }
   }

   @Override
   protected boolean validate(Void input)
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

   private static String commitDraft_;
}
