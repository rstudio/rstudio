/*
 * NumericValueWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.studio.client.RStudioGinjector;

public class NumericValueWidget extends Composite
      implements HasValue<String>,
                 HasEnsureVisibleHandlers
{
   public NumericValueWidget(String label)
   {
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.add(new SpanLabel(label, true));

      textBox_ = new TextBox();
      textBox_.setWidth("30px");
      textBox_.getElement().getStyle().setMarginLeft(0.6, Unit.EM);
      flowPanel.add(textBox_);

      initWidget(flowPanel);
   }

   public String getValue()
   {
      return textBox_.getValue();
   }

   public void setValue(String value)
   {
      textBox_.setValue(value);
   }

   public void setValue(String value, boolean fireEvents)
   {
      textBox_.setValue(value, fireEvents);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return textBox_.addValueChangeHandler(handler);
   }

   public boolean validate(String fieldName)
   {
      return validateRange(fieldName, null, null);
   }

   public boolean validatePositive(String fieldName)
   {
      return validateRange(fieldName, 1, null);
   }

   /**
    * Make sure field is a valid integer in the range [min, max). If min or max
    * are null, then 0 and infinity are assumed, respectively.
    */
   public boolean validateRange(String fieldName, Integer min, Integer max)
   {
      String value = textBox_.getValue().trim();
      if (!value.matches("^\\d+$"))
      {
         fireEvent(new EnsureVisibleEvent());
         textBox_.getElement().focus();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Error",
               fieldName + " must be a valid number.");
         return false;
      }
      if (min != null || max != null)
      {
         int intVal = Integer.parseInt(value);
         if (min != null && intVal < min)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Error",
                  fieldName + " must be greater than or equal to " + min + ".");
            return false;
         }
         if (max != null && intVal >= max)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Error",
                  fieldName + " must be less than " + max + ".");
            return false;
         }
      }
      return true;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   private TextBox textBox_;
}
