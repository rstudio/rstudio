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
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.VerticalSpacer;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
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
      String branch = tbBranch_.getValue().trim();
      String remote = sbRemote_.getValue().trim();
      boolean push = cbPush_.isVisible() ? cbPush_.getValue() : false;
      return new Input(branch, remote, push);
   }
   
   public CreateBranchDialog(final String caption,
                             final JsArrayString remotes,
                             final OperationWithInput<Input> operation)
   {
      super(caption, operation);
      
      setOkButtonCaption("Create");
      
      container_ = new VerticalPanel();
      tbBranch_ = createTextBox();
      tbBranch_.getElement().setAttribute("placeholder", "Branch name");
      
      String[] remoteLabels = new String[remotes.length() + 1];
      for (int i = 0; i < remotes.length(); i++)
         remoteLabels[i] = remotes.get(i);
      remoteLabels[remotes.length()] = REMOTE_NONE;
      
      sbRemote_ = new SelectWidget("Remote:", remoteLabels);
      sbRemote_.setValue(remoteLabels[0]);
      sbRemote_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            boolean isNone = sbRemote_.getValue().equals(REMOTE_NONE);
            cbPush_.setVisible(!isNone);
         }
      });
      
      final String remote = remotes.length() == 0 ? "(None)" : remotes.get(0);
      sbRemote_.setValue(remote);
      
      cbPush_ = new CheckBox("Push branch to remote");
      cbPush_.setVisible(!sbRemote_.getValue().equals(REMOTE_NONE));
      cbPush_.setValue(true);
      
      Grid grid = new Grid(1, 2);
      grid.setWidget(0, 0, new Label("Branch:"));
      grid.setWidget(0, 1, tbBranch_);;
      
      container_.add(grid);
      container_.add(new VerticalSpacer("6px"));
      container_.add(sbRemote_);
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
   private final SelectWidget sbRemote_;
   private final CheckBox cbPush_;
   
   private static final String REMOTE_NONE = "(None)";
}
