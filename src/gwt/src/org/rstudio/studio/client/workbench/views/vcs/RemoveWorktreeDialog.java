/*
 * RemoveWorktreeDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import java.util.List;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.common.vcs.WorktreeInfo;

public class RemoveWorktreeDialog extends ModalDialog<RemoveWorktreeDialog.Input>
{
   public static class Input
   {
      public Input(WorktreeInfo worktree, boolean force)
      {
         worktree_ = worktree;
         force_ = force;
      }

      public WorktreeInfo getWorktree()
      {
         return worktree_;
      }

      public boolean getForce()
      {
         return force_;
      }

      private final WorktreeInfo worktree_;
      private final boolean force_;
   }

   public RemoveWorktreeDialog(List<WorktreeInfo> worktrees,
                               OperationWithInput<Input> operation)
   {
      super(constants_.removeWorktreeCapitalized(), Roles.getDialogRole(), operation);
      setThemeAware(true);
      setOkButtonCaption(constants_.removeWorktreeCapitalized());

      worktrees_ = worktrees;

      lbWorktree_ = new ListBox();
      ElementIds.assignElementId(lbWorktree_, ElementIds.REMOVE_WORKTREE_SELECT);
      for (WorktreeInfo worktree : worktrees)
      {
         String label = worktree.getDisplayName() + " -- " + worktree.getPath();

         // a deleted directory leaves a stale entry behind, which removing
         // cleans up; say so, as the branch menu doesn't list it. A lock has
         // to be forced through, so that is worth knowing up front too.
         if (worktree.isPrunable())
            label += " " + constants_.worktreeMissing();
         if (worktree.isLocked())
            label += " " + constants_.worktreeLocked();

         lbWorktree_.addItem(label, worktree.getPath());
      }

      cbForce_ = new CheckBox(constants_.forceRemoveWorktree());
      ElementIds.assignElementId(cbForce_, ElementIds.REMOVE_WORKTREE_FORCE);

      container_ = new VerticalPanel();
      container_.add(new FormLabel(constants_.worktreeColon(), lbWorktree_));
      container_.add(lbWorktree_);
      container_.add(new VerticalSpacer("8px"));
      container_.add(cbForce_);
   }

   @Override
   protected Input collectInput()
   {
      int index = lbWorktree_.getSelectedIndex();
      if (index < 0 || index >= worktrees_.size())
         return null;

      return new Input(worktrees_.get(index), cbForce_.getValue());
   }

   @Override
   protected boolean validate(Input input)
   {
      return input != null;
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }

   @Override
   public void focusFirstControl()
   {
      lbWorktree_.setFocus(true);
   }

   private final List<WorktreeInfo> worktrees_;
   private final ListBox lbWorktree_;
   private final CheckBox cbForce_;
   private final VerticalPanel container_;

   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
