/*
 * NewWorktreeDialog.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;

public class NewWorktreeDialog extends ModalDialog<NewWorktreeDialog.Input>
{
   public static class Input
   {
      public Input(String path, String branch)
      {
         path_ = path;
         branch_ = branch;
      }

      public String getPath()
      {
         return path_;
      }

      public String getBranch()
      {
         return branch_;
      }

      private final String path_;
      private final String branch_;
   }

   // The worktree is created at <parent>/<directory name>; the directory
   // name follows the branch name until the user edits it by hand. The branch
   // is created unless one of that name exists already.
   public NewWorktreeDialog(String defaultParentDir,
                            OperationWithInput<Input> operation)
   {
      super(constants_.newWorktreeCapitalized(), Roles.getDialogRole(), operation);
      setThemeAware(true);

      setOkButtonCaption(constants_.createCapitalized());
      enableOkButton(false);

      tbBranch_ = new TextBox();
      tbBranch_.getElement().getStyle().setProperty("minWidth", "200px");
      Roles.getTextboxRole().setAriaRequiredProperty(tbBranch_.getElement(), true);
      ElementIds.assignElementId(tbBranch_, ElementIds.NEW_WORKTREE_BRANCH);

      // 'input' rather than a key handler, so that pasted text counts too
      DomUtils.addEventListener(tbBranch_.getElement(), "input", false, event -> onBranchChanged());

      tbDirectoryName_ = new TextBox();
      tbDirectoryName_.getElement().getStyle().setProperty("minWidth", "200px");
      ElementIds.assignElementId(tbDirectoryName_, ElementIds.NEW_WORKTREE_DIRECTORY);
      DomUtils.addEventListener(tbDirectoryName_.getElement(), "input", false, event ->
      {
         directoryNameEdited_ = true;
         updateOkButton();
      });

      dirParent_ = new DirectoryChooserTextBox(
            constants_.createInColon(),
            ElementIds.TextBoxButtonId.WORKTREE_PARENT,
            tbBranch_);
      dirParent_.addValueChangeHandler(event -> updateOkButton());
      dirParent_.setText(defaultParentDir);

      LayoutGrid grid = new LayoutGrid(2, 2);
      grid.setWidth("100%");
      grid.setWidget(0, 0, new FormLabel(constants_.branchNameColon(), tbBranch_));
      grid.setWidget(0, 1, tbBranch_);
      grid.setWidget(1, 0, new FormLabel(constants_.directoryNameColon(), tbDirectoryName_));
      grid.setWidget(1, 1, tbDirectoryName_);

      container_ = new VerticalPanel();
      container_.add(grid);
      container_.add(new VerticalSpacer("6px"));
      container_.add(dirParent_);
   }

   @Override
   protected Input collectInput()
   {
      String parent = dirParent_.getText().trim();
      String name = tbDirectoryName_.getValue().trim();
      String path = FileSystemItem.createDir(parent).completePath(name);
      return new Input(path, tbBranch_.getValue().trim());
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

   private void onBranchChanged()
   {
      if (!directoryNameEdited_)
         tbDirectoryName_.setValue(directoryNameForBranch(tbBranch_.getValue()));

      updateOkButton();
   }

   private void updateOkButton()
   {
      boolean ok = !StringUtil.isNullOrEmpty(tbBranch_.getValue().trim()) &&
                   !StringUtil.isNullOrEmpty(tbDirectoryName_.getValue().trim()) &&
                   !StringUtil.isNullOrEmpty(dirParent_.getText().trim());
      enableOkButton(ok);
   }

   // 'feature/foo' can't be a directory name, so flatten path separators
   public static String directoryNameForBranch(String branch)
   {
      return branch.trim().replaceAll("[/\\\\]+", "-");
   }

   private final TextBox tbBranch_;
   private final TextBox tbDirectoryName_;
   private final DirectoryChooserTextBox dirParent_;
   private final VerticalPanel container_;
   private boolean directoryNameEdited_ = false;

   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
