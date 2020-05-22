/*
 * AppNameTextbox.java
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.rsconnect.model.RSConnectAppName;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class AppNameTextbox extends Composite
{

   private static AppNameTextboxUiBinder uiBinder = GWT
         .create(AppNameTextboxUiBinder.class);

   interface AppNameTextboxUiBinder extends UiBinder<Widget, AppNameTextbox>
   {
   }
   
   interface Host
   {
      boolean supportsTitle();
      void generateAppName(String title, 
                           CommandWithArg<RSConnectAppName> result);
   }

   public AppNameTextbox(Host host)
   {
      host_ = host;
      initWidget(uiBinder.createAndBindUi(this));

      // Validate the application name on every keystroke
      appTitle_.addKeyUpHandler(new KeyUpHandler()
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
      onNameIsInvalidTitle_ = cmd;
   }
   
   public void setTitle(String text)
   {
      appTitle_.setText(text);
   }
   
   public String getTitle()
   {
      return appTitle_.getText().trim();
   }
   
   public TextBox getTextBox()
   {
      return appTitle_;
   }
   
   public String getName()
   {
      // return current title if no generated name is available
      if (StringUtil.isNullOrEmpty(name_))
         return getTitle();
      return name_;
   }
   
   public void setFocus(boolean focused)
   {
      appTitle_.setFocus(focused);
   }
   
   public void validateAppName()
   {
      if (!host_.supportsTitle())
      {
         String app = appTitle_.getText();
         RegExp validReg = RegExp.compile("^[A-Za-z0-9_-]{4,63}$");
         validTitle_ = validReg.test(app);
         setAppNameValid(validTitle_);
         if (validTitle_)
            name_ = app;
         else
            error_.setText("The title must contain 4 - 64 alphanumeric " + 
                           "characters.");
         return;
      }

      // if we don't have enough characters, bail out early 
      final String title = appTitle_.getText().trim();
      if (title.length() < 3)
      {
         validTitle_ = false;
         // if we also don't have focus in the box, show an error
         if (DomUtils.getActiveElement() != appTitle_.getElement())
         {
            setAppNameValid(false);
            error_.setText("The title must contain at least 3 characters.");
         }
         return;
      }

      host_.generateAppName(title, 
                            new CommandWithArg<RSConnectAppName>()
         {
            @Override
            public void execute(RSConnectAppName arg)
            {
               name_ = arg.name();
               validTitle_ = arg.valid();
               error_.setText(arg.error());
               setAppNameValid(arg.valid());
            }
         });
   }
   
   @Override
   public void setStyleName(String styleName)
   {
      appTitle_.setStyleName(styleName);
   }
   
   public void setDetails(String title, String name)
   {
      if (StringUtil.isNullOrEmpty(title))
         title = name;
      appTitle_.setTitle(title);
      name_ = name;
   }

   public boolean isValid()
   {
      return !appTitle_.getText().trim().isEmpty() && validTitle_;
   }
   
   // Private methods ---------------------------------------------------------
   
   private void setAppNameValid(boolean isValid)
   {
      nameValidatePanel_.setVisible(!isValid);
      if (isValid && onNameIsValid_ != null)
         onNameIsValid_.execute();
      else if (!isValid && onNameIsInvalidTitle_ != null)
         onNameIsInvalidTitle_.execute();
   }

   private final Host host_;
   private Command onNameIsValid_;
   private Command onNameIsInvalidTitle_;
   private String name_;
   private boolean validTitle_ = true;
   
   @UiField TextBox appTitle_;
   @UiField HTMLPanel nameValidatePanel_;
   @UiField Label error_;
}
