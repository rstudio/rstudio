/*
 * TextBoxWithButton.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeResources;

public class TextBoxWithButton extends Composite
                               implements HasValueChangeHandlers<String>,
                                          CanFocus
{
   /**
    * @param label label text
    * @param emptyLabel placeholder text
    * @param action button text
    * @param helpButton optional HelpButton
    * @param uniqueId unique elementId for this instance
    * @param readOnly textbox editability
    * @param handler button click callback
    */
   public TextBoxWithButton(String label,
                            String emptyLabel,
                            String action,
                            HelpButton helpButton,
                            ElementIds.TextBoxButtonId uniqueId,
                            boolean readOnly,
                            ClickHandler handler)
   {
      this(label, null, emptyLabel, action, helpButton, uniqueId, readOnly, handler);
   }

   /**
    * @param existingLabel label control to associate with textbox
    * @param emptyLabel placeholder text
    * @param action button text
    * @param uniqueId unique elementId for this instance
    * @param readOnly textbox editability
    * @param handler button click callback
    */
   public TextBoxWithButton(FormLabel existingLabel,
                            String emptyLabel,
                            String action,
                            ElementIds.TextBoxButtonId uniqueId,
                            boolean readOnly,
                            ClickHandler handler)
   {
      this(null, existingLabel, emptyLabel, action, null, uniqueId, readOnly, handler);
   }

   protected TextBoxWithButton(String label,
                               FormLabel existingLabel,
                               String emptyLabel,
                               String action,
                               HelpButton helpButton,
                               ElementIds.TextBoxButtonId uniqueId,
                               boolean readOnly,
                               ClickHandler handler)
   {
      emptyLabel_ = StringUtil.isNullOrEmpty(emptyLabel) ? "" : emptyLabel;
      uniqueId_ = "_" + uniqueId;
      
      textBox_ = new TextBox();
      textBox_.setWidth("100%");
      textBox_.setReadOnly(readOnly);
      
      textBox_.addValueChangeHandler((ValueChangeEvent<String> event) ->
      {
         ValueChangeEvent.fire(TextBoxWithButton.this, getText());
      });

      themedButton_ = new ThemedButton(action, handler);

      // prevent button from triggering "submit" when hosted in a form, such as in FileUploadDialog
      themedButton_.getElement().setAttribute("type", "button");

      inner_ = new HorizontalPanel();
      inner_.add(textBox_);
      inner_.add(themedButton_);
      inner_.setCellWidth(textBox_, "100%");
      inner_.setWidth("100%");

      FlowPanel outer = new FlowPanel();
      if (label != null)
      {
         assert existingLabel == null : "Invalid usage, cannot provide both label and existingLabel";

         lblCaption_ = new FormLabel(label, true);
         if (helpButton != null)
         {
            helpButton_ = helpButton;
            HorizontalPanel panel = new HorizontalPanel();
            panel.add(lblCaption_);
            helpButton.getElement().getStyle().setMarginLeft(5, Unit.PX);
            panel.add(helpButton);
            outer.add(panel);
         }
         else
         {
            outer.add(lblCaption_);
         }
      }
      else
      {
         lblCaption_ = existingLabel;
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
      String oldText = getText();
      
      if (StringUtil.equals(oldText, useDefaultValue_))
      {
         textBox_.setText(USE_DEFAULT_PREFIX + " " + text);
      }
      else if (text.length() > 0)
      {
         textBox_.setText(text);
      }
      else
      {
         textBox_.setText(emptyLabel_);
      }

      ValueChangeEvent.fire(this, getText());
   }

   public String getText()
   {
      String text = textBox_.getText();
      
      if (StringUtil.equals(text, emptyLabel_))
         return "";
      
      if (text.startsWith(USE_DEFAULT_PREFIX))
      {
         text = text.substring(USE_DEFAULT_PREFIX.length()).trim();
      }
      
      return text;
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

   public TextBox getTextBox()
   {
      return textBox_;
   }

   protected ThemedButton getButton()
   {
      return themedButton_;
   }

   @Override
   public void focus()
   {
      textBox_.setFocus(true);
   }
   
   public void blur()
   {
      textBox_.setFocus(false);
   }

   @Override
   protected void onAttach()
   {
      super.onAttach();

      // Some UI scenarios create multiple TextBoxWithButtons before adding them to the
      // DOM; defer assigning IDs until added to DOM in order to detect and
      // prevent duplicates.
      ElementIds.assignElementId(textBox_, ElementIds.TBB_TEXT + uniqueId_);
      ElementIds.assignElementId(themedButton_, ElementIds.TBB_BUTTON + uniqueId_);
      if (helpButton_ != null)
         ElementIds.assignElementId(helpButton_, ElementIds.TBB_HELP + uniqueId_);
      if (lblCaption_ != null)
         lblCaption_.setFor(textBox_);
   }


   private final HorizontalPanel inner_;
   private FormLabel lblCaption_;
   private final TextBox textBox_;
   private HelpButton helpButton_;
   private final ThemedButton themedButton_;
   private final String emptyLabel_;
   private String useDefaultValue_;
   private String uniqueId_;
   
   private static final String USE_DEFAULT_PREFIX = "[Use Default]";
}
