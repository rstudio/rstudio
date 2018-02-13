/*
 * AskSecretDialog.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

package org.rstudio.studio.client.common.rstudioapi.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

public class AskSecretDialog extends ModalDialog<AskSecretDialogResult>
{
   interface Binder extends UiBinder<Widget, AskSecretDialog>
   {}

   public AskSecretDialog(String title,
                          String prompt,
                          ProgressOperationWithInput<AskSecretDialogResult> okOperation,
                          Operation cancelOperation)
   {
      super(title, okOperation, cancelOperation);

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
     
      label_.setText(prompt);
      textbox_.setFocus(true);
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   protected AskSecretDialogResult collectInput()
   {
      AskSecretDialogResult result = new AskSecretDialogResult(textbox_.getText(), true);

      return result;
   }

   @Override
   protected void onDialogShown()
   {
      textbox_.setFocus(true);
   }

   @Override
   protected boolean validate(AskSecretDialogResult input)
   {
      if (StringUtil.isNullOrEmpty(input.getSecret()))
      {
         MessageDialog dialog = new MessageDialog(MessageDialog.ERROR,
                                                  "Error",
                                                  "You must enter a value.");
         dialog.addButton("OK", (Operation)null, true, true);
         dialog.showModal();
         textbox_.setFocus(true);

         return false;
      }
      
      return true;
   }

   @Override
   protected void positionAndShowDialog(final Command onCompleted)
   {
     setPopupPositionAndShow(new PositionCallback() {

        @Override
        public void setPosition(int offsetWidth, int offsetHeight)
        {
           int left = (Window.getClientWidth()/2) - (offsetWidth/2);
           int top = 50;

           setPopupPosition(left, top);
           onCompleted.execute();
        }

     });
   }

   private Widget mainWidget_;

   @UiField Label label_;
   @UiField PasswordTextBox textbox_;
   @UiField CheckBox remember_;
}
