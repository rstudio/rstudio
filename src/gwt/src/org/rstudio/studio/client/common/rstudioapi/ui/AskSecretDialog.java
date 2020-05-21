/*
 * AskSecretDialog.java
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

package org.rstudio.studio.client.common.rstudioapi.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.dependencies.DependencyManager;

public class AskSecretDialog extends ModalDialog<AskSecretDialogResult>
{
   interface Binder extends UiBinder<Widget, AskSecretDialog>
   {}

   public AskSecretDialog(String title,
                          String prompt,
                          boolean canRemember,
                          boolean hasSecret,
                          DependencyManager dependencyManager,
                          ProgressOperationWithInput<AskSecretDialogResult> okOperation,
                          Operation cancelOperation)
   {
      super(title, Roles.getDialogRole(), okOperation, cancelOperation);

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
     
      setHelpLink(new HelpLink(
         "Using Keyring",
         "using_keyring",
         false,
         true)
      );

      stillHasSecret_ = hasSecret;
      if (hasSecret) {
        // set some data to represent an existing value
        textbox_.setText(secretPlaceholder_);
        hasChanged_ = false;
        remember_.setValue(true);
      }
      else {
        hasChanged_ = true;
      }
      
      textbox_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            hasChanged_ = true;
         }
      });
      
      textbox_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (stillHasSecret_)
            {
               stillHasSecret_ = false;
               textbox_.setText("");
            }
         }
      });

      label_.setText(prompt);
      Roles.getTextboxRole().setAriaRequiredProperty(textbox_.getElement(), true);

      install_.setClickHandler(() ->
      {
         VerticalPanel verticalPanel = new VerticalPanel();
         verticalPanel.getElement().setAttribute(
            "style",
            "padding-left: 6px; width: 320px;"
         );

         Label infoLabel = new Label(
            "Keyring is an R package that provides access to " +
            "the operating systems credential store to allow you " +
            "to remember, securely, passwords and secrets. "
         );

         HTML questionHtml = new HTML(
            "<br>Would you like to install keyring?<br><br>"
         );

         verticalPanel.add(infoLabel);
         verticalPanel.add(questionHtml);

         MessageDialog dialog = new MessageDialog(
            MessageDialog.QUESTION,
            "Keyring",
            verticalPanel);

         dialog.addButton("Install", ElementIds.DIALOG_OK_BUTTON, new Operation()
         {
            @Override
            public void execute()
            {
               dependencyManager.withKeyring(
                  new Command()
                  {
                     @Override
                     public void execute()
                     {
                        enableKeyring(true);
                     }
                  }
               );
            }
         }, true, false);

         dialog.addButton("Cancel", ElementIds.DIALOG_CANCEL_BUTTON, (Operation)null, false, true);
         dialog.showModal();
      });


      enableKeyring(canRemember);
   }
   
   private void enableKeyring(boolean canRemember)
   {
      rememberEnabled_.setVisible(false);
      rememberDisabled_.setVisible(false);

      if (canRemember)
        rememberEnabled_.setVisible(true);
      else
        rememberDisabled_.setVisible(true);
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   protected AskSecretDialogResult collectInput()
   {
      if (!hasChanged_ && textbox_.getText() != secretPlaceholder_) {
        hasChanged_ = true;
      }

      AskSecretDialogResult result = new AskSecretDialogResult(
        textbox_.getText(),
        remember_.getValue(),
        hasChanged_
      );

      return result;
   }

   @Override
   protected void focusInitialControl()
   {
      textbox_.setFocus(true);
      textbox_.setSelectionRange(0, textbox_.getText().length());
   }

   @Override
   protected boolean validate(AskSecretDialogResult input)
   {
      if (StringUtil.isNullOrEmpty(input.getSecret()))
      {
         MessageDialog dialog = new MessageDialog(MessageDialog.ERROR,
                                                  "Error",
                                                  "You must enter a value.");
         dialog.addButton("OK", ElementIds.DIALOG_OK_BUTTON, (Operation)null, true, true);
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

   @UiField FormLabel label_;
   @UiField PasswordTextBox textbox_;
   @UiField CheckBox remember_;
   @UiField HyperlinkLabel install_;

   @UiField HTMLPanel rememberEnabled_;
   @UiField HTMLPanel rememberDisabled_;

   private boolean hasChanged_ = false;
   private boolean stillHasSecret_ = false;
   private static String secretPlaceholder_ = "00000000";
}
