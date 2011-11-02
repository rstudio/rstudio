/*
 * TextBoxWithButton.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.theme.res.ThemeResources;

public class TextBoxWithButton extends Composite 
{
   public TextBoxWithButton(String label, String action, ClickHandler handler)
   {
      this(label, "", action, handler);
   }
   
   public TextBoxWithButton(String label, 
                            String emptyLabel, 
                            String action, 
                            ClickHandler handler)
   {
      emptyLabel_ = emptyLabel;
      
      textBox_ = new TextBox();
      textBox_.setWidth("100%");
      textBox_.setReadOnly(true);

      themedButton_ = new ThemedButton(action, handler);

      inner_ = new HorizontalPanel();
      inner_.add(textBox_);
      inner_.add(themedButton_);
      inner_.setCellWidth(textBox_, "100%");
      inner_.setWidth("100%");

      FlowPanel outer = new FlowPanel();
      if (label != null)
      {
         outer.add(new Label(label, true));
      }
      outer.add(inner_);
      initWidget(outer);

      addStyleName(ThemeResources.INSTANCE.themeStyles().textBoxWithButton());
   }
   
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return themedButton_.addClickHandler(handler);
   }
   
   public void focusButton()
   {
      themedButton_.setFocus(true);
   }

   public void setText(String text)
   {
      if (text.length() > 0)
         textBox_.setText(text);
      else
         textBox_.setText(emptyLabel_);
   }

   public String getText()
   {
      String text = textBox_.getText();
      if (!text.equals(emptyLabel_))
         return text;
      else
         return "";
   }
   
   public void setTextWidth(String width)
   {
      inner_.setCellWidth(textBox_, width);
   }

   public void click()
   {
      themedButton_.click();
   }

   public boolean isEnabled()
   {
      return themedButton_.isEnabled();
   }

   public void setEnabled(boolean enabled)
   {
      textBox_.setEnabled(enabled);
      themedButton_.setEnabled(enabled);
   }

   private HorizontalPanel inner_;
   private TextBox textBox_;
   private ThemedButton themedButton_;
   private String emptyLabel_;
}
