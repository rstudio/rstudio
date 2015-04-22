/*
 * AppNameTextbox.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class AppNameTextbox extends Composite
{

   private static AppNameTextboxUiBinder uiBinder = GWT
         .create(AppNameTextboxUiBinder.class);

   interface AppNameTextboxUiBinder extends UiBinder<Widget, AppNameTextbox>
   {
   }

   public AppNameTextbox()
   {
      initWidget(uiBinder.createAndBindUi(this));

      // Validate the application name on every keystroke
      appName_.addKeyUpHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            validateAppName();
         }
      });
   }
   
   public void setOnNameIsValid(Command cmd)
   {
      onNameIsValid_ = cmd;
   }
   
   public void setOnNameIsInvalid(Command cmd)
   {
      onNameIsInvalid_ = cmd;
   }
   
   public void setText(String text)
   {
      appName_.setText(text);
   }
   
   public String getText()
   {
      return appName_.getText();
   }
   
   public void setFocus(boolean focused)
   {
      appName_.setFocus(focused);
   }
   
   public boolean validateAppName()
   {
      String app = appName_.getText();
      RegExp validReg = RegExp.compile("^[A-Za-z0-9_-]{4,63}$");
      boolean isValid = validReg.test(app);
      setAppNameValid(isValid);
      return isValid;
   }
   
   @Override
   public void setStyleName(String styleName)
   {
      appName_.setStyleName(styleName);
   }
   
   // Private methods ---------------------------------------------------------
   
   private void setAppNameValid(boolean isValid)
   {
      nameValidatePanel_.setVisible(!isValid);
      if (isValid && onNameIsValid_ != null)
         onNameIsValid_.execute();
      else if (!isValid && onNameIsInvalid_ != null)
         onNameIsInvalid_.execute();
   }

   private Command onNameIsValid_;
   private Command onNameIsInvalid_;
   
   @UiField TextBox appName_;
   @UiField HTMLPanel nameValidatePanel_;
}
