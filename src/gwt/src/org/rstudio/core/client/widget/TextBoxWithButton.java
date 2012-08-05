/*
 * TextBoxWithButton.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.theme.res.ThemeResources;

public class TextBoxWithButton extends Composite
                               implements HasValueChangeHandlers<String>,
                                          CanFocus
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
      this(label, emptyLabel, action, null, handler);
   }
   
   public TextBoxWithButton(String label, 
                            String emptyLabel, 
                            String action, 
                            HelpButton helpButton,
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
         Label lblCaption = new Label(label, true);
         if (helpButton != null)
         {
            HorizontalPanel panel = new HorizontalPanel();
            panel.add(lblCaption);
            helpButton.getElement().getStyle().setMarginLeft(5, Unit.PX);
            panel.add(helpButton);
            outer.add(panel);
         }
         else
         {
            outer.add(lblCaption);
         }
      }
      outer.add(inner_);
      initWidget(outer);

      addStyleName(ThemeResources.INSTANCE.themeStyles().textBoxWithButton());
   }
   
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return themedButton_.addClickHandler(handler);
   }
   
   public HandlerRegistration addValueChangeHandler(
                                    ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }
  
   
   public void focusButton()
   {
      themedButton_.setFocus(true);
   }

   // use a special adornment when the displayed key matches an 
   // arbitrary default value
   public void setUseDefaultValue(String useDefaultValue)
   {
      useDefaultValue_ = useDefaultValue;
   }
   
   public void setText(String text)
   {
      text_ = text;
      
      if (text_.equals(useDefaultValue_))
         textBox_.setText("[Use Default] " + text);
      else if (text.length() > 0)
         textBox_.setText(text);
      else
         textBox_.setText(emptyLabel_);
      
      ValueChangeEvent.fire(this, getText());
   }

   public String getText()
   {
      return text_;
   }
   
   public void setTextWidth(String width)
   {
      inner_.setCellWidth(textBox_, width);
   }

   public void setReadOnly(boolean readOnly)
   {
      textBox_.setReadOnly(readOnly);
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
   
   protected TextBox getTextBox()
   {
      return textBox_;
   }
   
   @Override
   public void focus()
   {
      textBox_.setFocus(true);
   }

   private HorizontalPanel inner_;
   private TextBox textBox_;
   private ThemedButton themedButton_;
   private String emptyLabel_;
   private String useDefaultValue_;
   private String text_ = "";
  
}
