/*
 * LabeledTextBox.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TextBox;
import org.rstudio.core.client.dom.DomUtils;

/**
 * A TextBox with an associated label.
 */
public class LabeledTextBox extends Composite
                            implements HasText, HasKeyUpHandlers
{
   public @UiConstructor LabeledTextBox(String textBoxId)
   {
      FlowPanel flowPanel = new FlowPanel();
      label_ = new FormLabel();
      textBox_ = new TextBox();
      textBox_.getElement().setId(textBoxId);
      label_.setFor(textBox_);
      setLabelInline(false);
      flowPanel.add(label_);
      flowPanel.add(textBox_);

      initWidget(flowPanel);
   }

   public void setLabelText(String label)
   {
      label_.setText(label);
   }

   @Override
   public void setText(String text)
   {
      textBox_.setText(text);
   }

   @Override
   public String getText()
   {
      return textBox_.getText();
   }

   public void setFocus(boolean focused)
   {
      textBox_.setFocus(focused);
   }

   public void selectAll()
   {
      textBox_.selectAll();
   }

   public void setTextRequired(boolean required)
   {
      Roles.getTextboxRole().setAriaRequiredProperty(textBox_.getElement(), required);
   }

   public void disableAutoBehavior()
   {
      DomUtils.disableAutoBehavior(textBox_);
   }

   public void setEnableSpellcheck(boolean enable)
   {
      textBox_.getElement().setAttribute("spellcheck", enable ? "true" : "false");
   }

   /**
    * @return underlying TextBox control
    */
   public TextBox getTextBox()
   {
      return textBox_;
   }

   /**
    * @return underlying FormLabel control
    */
   public FormLabel getLabel()
   {
      return label_;
   }

   /**
    * @param inline true to have label inline with TextBox
    */
   public void setLabelInline(boolean inline)
   {
      // by default a label element is inline
      if (!inline)
         label_.getElement().getStyle().setDisplay(Display.BLOCK);
      else
         label_.getElement().getStyle().clearDisplay();
   }

   @Override
   public HandlerRegistration addKeyUpHandler(KeyUpHandler handler)
   {
      return textBox_.addKeyUpHandler(handler);
   }

   public void setEnabled(boolean enabled)
   {
      textBox_.setEnabled(enabled);
   }

   public void setLabelStyleName(String style)
   {
      label_.setStyleName(style);
   }

   public void setTextBoxStyleName(String style)
   {
      textBox_.setStyleName(style);
   }

   FormLabel label_;
   TextBox textBox_;
}
