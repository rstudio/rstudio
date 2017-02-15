/*
 * CreateBranchDialog.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.vcs;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CreateBranchDialog extends ModalDialog<CreateBranchDialog.Input>
{
   public static class Input
   {
      public Input(String branch, String remote, boolean push)
      {
         branch_ = branch;
         remote_ = remote;
         push_ = push;
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
   }

   @Override
   protected Input collectInput()
   {
      return new Input(
            tbBranch_.getValue().trim(),
            tbRemote_.getValue().trim(),
            cbPush_.getValue());
   }
   
   public CreateBranchDialog(final String caption,
                             final JsArrayString remotes,
                             final OperationWithInput<Input> operation)
   {
      super(caption, operation);
      
      setOkButtonCaption("Create");
      
      container_ = new VerticalPanel();
      tbBranch_ = createTextBox();
      tbRemote_ = createTextBox();
      cbPush_ = new CheckBox("Push branch to remote");
      
      final String remote = remotes.length() == 0 ? "origin" : remotes.get(0);
      tbRemote_.setValue(remote);
      
      cbPush_.setValue(true);
      
      Grid grid = new Grid(2, 2);
      grid.setWidget(0, 0, new Label("Branch:"));
      grid.setWidget(0, 1, tbBranch_);
      grid.setWidget(1, 0, new Label("Remote:"));
      grid.setWidget(1, 1, tbRemote_);
      
      container_.add(grid);
      container_.add(new VerticalSpacer("12px"));
      container_.add(cbPush_);
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   @Override
   public void showModal()
   {
      super.showModal();
      tbBranch_.setFocus(true);
   }
   
   private TextBox createTextBox()
   {
      TextBox textBox = new TextBox();
      textBox.setWidth("200px");
      textBox.getElement().getStyle().setPaddingLeft(3, Unit.PX);
      textBox.getElement().getStyle().setPaddingRight(3, Unit.PX);
      textBox.getElement().getStyle().setPaddingBottom(2, Unit.PX);
      textBox.getElement().getStyle().setPaddingTop(2, Unit.PX);
      return textBox;
   }
   
   private final VerticalPanel container_;
   private final TextBox tbBranch_;
   private final TextBox tbRemote_;
   private final CheckBox cbPush_;
}
