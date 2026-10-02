/*
 * CreateBranchDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Functional;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.Functional.Predicate;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.common.vcs.RemotesInfo;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CreateBranchDialog extends ModalDialog<CreateBranchDialog.Input>
{
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
   public static class Input
   {
      public Input(String branch, String remote, boolean push, String worktreeParent)
      {
         branch_ = branch;
         remote_ = remote;
         push_ = push;
         worktreeParent_ = worktreeParent;
      }

      // Directory to create a worktree for the branch in, or null to check
      // the branch out here
      public final String getWorktreeParent()
      {
         return worktreeParent_;
      }
      
      public final String getBranch()
      {
         return branch_;
      }
      
      public final String getRemote()
      {
         return remote_;
      }
      
      public final boolean getPush()
      {
         return push_;
      }
      
      private final String branch_;
      private final String remote_;
      private final boolean push_;
      private final String worktreeParent_;
   }

   @Override
   protected Input collectInput()
   {
      String branch = tbBranch_.getValue().trim();
      String remote = sbRemote_.getSelectedValue().trim();
      boolean push = cbPush_.isVisible() ? cbPush_.getValue() : false;
      String worktreeParent = cbWorktree_.getValue() ? dirWorktreeParent_.getText().trim() : null;
      return new Input(branch, remote, push, worktreeParent);
   }
   
   public CreateBranchDialog(final String caption,
                             final JsArray<RemotesInfo> remotesInfo,
                             final String worktreeParentDir,
                             final OperationWithInput<CreateBranchDialog.Input> onCreateBranch,
                             final OperationWithInput<AddRemoteDialog.Input> onAddRemote)
   {
      super(caption, Roles.getDialogRole(), onCreateBranch);
      setThemeAware(true);

      setOkButtonCaption(constants_.createCapitalized());
      enableOkButton(false);
      
      container_ = new VerticalPanel();
      
      tbBranch_ = textBox();
      ElementIds.assignElementId(tbBranch_, ElementIds.NEW_BRANCH_NAME);
      Roles.getTextboxRole().setAriaRequiredProperty(tbBranch_.getElement(), true);
      tbBranch_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  String text = tbBranch_.getValue();
                  enableOkButton(!StringUtil.isNullOrEmpty(text));
               }
            });
         }
      });
      
      // a bare ListBox, so it lines up with the branch name box in the grid
      // (SelectWidget adds its own margins)
      sbRemote_ = new ListBox();
      sbRemote_.setWidth("100%");
      sbRemote_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            boolean isNone = sbRemote_.getSelectedValue() == REMOTE_NONE;
            cbPush_.setVisible(!isNone);
         }
      });
      
      btnAddRemote_ = new ThemedButton(constants_.addRemoteEllipses());
      btnAddRemote_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            // Try to use the remote URL associated with the currently
            // selected remote name if available.
            String remoteUrl = null;
            if (remotesInfo_ != null)
            {
               final String currentRemote = sbRemote_.getSelectedValue();
               RemotesInfo info = Functional.find(remotesInfo_, new Predicate<RemotesInfo>()
               {
                  @Override
                  public boolean test(RemotesInfo info)
                  {
                     return info.getRemote() == currentRemote;
                  }
               });

               if (info != null)
                  remoteUrl = info.getUrl();
            }
            
            AddRemoteDialog dialog = new AddRemoteDialog(
                  constants_.addRemote(),
                  remoteUrl,
                  onAddRemote);
            
            dialog.showModal();
         }
      });
      
      addLeftButton(btnAddRemote_, ElementIds.NEW_BRANCH_ADD_REMOTE);

      cbPush_ = new CheckBox(constants_.syncBranchWithRemote());
      cbPush_.setVisible(sbRemote_.getSelectedValue() != REMOTE_NONE);
      cbPush_.setValue(true);
      
      setRemotes(remotesInfo);
      
      // branch name and remote share a grid so their inputs line up
      LayoutGrid grid = new LayoutGrid(2, 2);
      grid.setWidth("100%");
      grid.setWidget(0, 0, new FormLabel(constants_.branchNameColon(), tbBranch_));
      grid.setWidget(0, 1, tbBranch_);
      tbBranch_.setWidth("100%");
      grid.setWidget(1, 0, new FormLabel(constants_.remoteColon(), sbRemote_));
      grid.setWidget(1, 1, sbRemote_);
      grid.getCellFormatter().getElement(1, 1).getStyle().setPaddingTop(6, Unit.PX);

      container_.add(grid);
      container_.add(new VerticalSpacer("6px"));
      container_.add(cbPush_);

      // optionally check the new branch out into a worktree of its own, at
      // <parent>/<branch name with separators flattened>
      cbWorktree_ = new CheckBox(constants_.createInNewWorktree());
      ElementIds.assignElementId(cbWorktree_, ElementIds.NEW_BRANCH_WORKTREE);
      dirWorktreeParent_ = new DirectoryChooserTextBox(
            constants_.createInColon(),
            ElementIds.TextBoxButtonId.NEW_BRANCH_WORKTREE_PARENT,
            tbBranch_);
      dirWorktreeParent_.setText(worktreeParentDir);
      dirWorktreeParent_.getElement().getStyle().setPaddingTop(4, Unit.PX);
      dirWorktreeParent_.setVisible(false);
      cbWorktree_.addValueChangeHandler(event -> dirWorktreeParent_.setVisible(event.getValue()));

      container_.add(cbWorktree_);
      container_.add(dirWorktreeParent_);
   }
   
   public void setRemotes(JsArray<RemotesInfo> remotesInfo)
   {
      setRemotes(null, remotesInfo);
   }
   
   public void setRemotes(String activeRemote,
                          JsArray<RemotesInfo> remotesInfo)
   {
      remotesInfo_ = remotesInfo;
      
      // one entry per remote (git lists fetch and push URLs separately),
      // labelled with its URL so similarly named remotes can be told apart
      List<String> remotes = new ArrayList<>();
      List<String> labels = new ArrayList<>();
      for (RemotesInfo info : JsUtil.asIterable(remotesInfo))
      {
         if (!remotes.contains(info.getRemote()))
         {
            String remote = info.getRemote();
            remotes.add(remote);
            labels.add(StringUtil.isNullOrEmpty(info.getUrl())
                  ? remote
                  : remote + " (" + info.getUrl() + ")");
            if (activeRemote == null && info.isActive())
               activeRemote = remote;
         }
      }
      
      String[] choices = new String[remotes.size() + 1];
      String[] choiceLabels = new String[remotes.size() + 1];
      for (int i = 0; i < remotes.size(); i++)
      {
         choices[i] = remotes.get(i);
         choiceLabels[i] = labels.get(i);
      }
      choices[remotes.size()] = REMOTE_NONE;
      choiceLabels[remotes.size()] = REMOTE_NONE;
      
      // if we haven't set an active remote, try defaulting to the one called
      // 'origin' (if it exists)
      if (activeRemote == null)
      {
         for (int i = 0; i < choices.length; i++)
         {
            if (REMOTE_ORIGIN == choices[i])
            {
               activeRemote = REMOTE_ORIGIN;
               break;
            }
         }
      }
      
      // if we still haven't found anything, just default to the first entry
      // (note that because we always add the (none) remote there will always
      // be an entry available here)
      if (activeRemote == null)
         activeRemote = choices[0];
      
      sbRemote_.clear();
      for (int i = 0; i < choices.length; i++)
      {
         sbRemote_.addItem(choiceLabels[i], choices[i]);
         if (choices[i] == activeRemote)
            sbRemote_.setSelectedIndex(i);
      }
      
      cbPush_.setVisible(choices.length > 1);
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   @Override
   public void focusFirstControl()
   {
      tbBranch_.setFocus(true);
      tbBranch_.selectAll();
   }
   
   private final CheckBox cbWorktree_;
   private final DirectoryChooserTextBox dirWorktreeParent_;

   private TextBox textBox()
   {
      TextBox textBox = new TextBox();
      textBox.getElement().getStyle().setProperty("minWidth", "200px");
      DomUtils.disableSpellcheck(textBox);
      return textBox;
   }
   
   private JsArray<RemotesInfo> remotesInfo_;
   
   private final VerticalPanel container_;
   private final TextBox tbBranch_;
   private final ListBox sbRemote_;
   private final ThemedButton btnAddRemote_;
   private final CheckBox cbPush_;
   
   private static final String REMOTE_NONE = "(None)";
   private static final String REMOTE_ORIGIN = "origin";
}
