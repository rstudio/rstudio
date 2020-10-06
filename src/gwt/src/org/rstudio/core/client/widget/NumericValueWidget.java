/*
 * NumericValueWidget.java
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
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.studio.client.RStudioGinjector;

public class NumericValueWidget extends Composite
      implements HasValue<String>,
                 HasEnsureVisibleHandlers,
                 CanSetControlId
{
   public static final Integer ZeroMinimum = null;
   public static final Integer NoMaximum = null;

   public NumericValueWidget()
   {
      this("", ZeroMinimum, NoMaximum);
   }

   /**
    * Prompt for an integer in the range [min, max]
    *
    * @param label
    * @param minValue minimum, if null (ZeroMinimum), zero assumed
    * @param maxValue maximum, if null (NoMaximum), no maximum assumed
    */
   public NumericValueWidget(String label, Integer minValue, Integer maxValue)
   {
      label_ = label;
      FlowPanel flowPanel = new FlowPanel();

      textBox_ = new NumericTextBox();
      textBox_.setWidth("48px");
      setLimits(minValue, maxValue);
      textBox_.getElement().getStyle().setMarginLeft(0.6, Unit.EM);
      flowPanel.add(textBoxLabel_ = new SpanLabel(label_, textBox_, true));
      flowPanel.add(textBox_);

      initWidget(flowPanel);
   }

   public String getLabel()
   {
      return textBoxLabel_.getText();
   }

   public void setLabel(String text)
   {
      label_ = text;
      textBoxLabel_.setText(text);
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

   public void setLimits(Integer minValue, Integer maxValue)
   {
      minValue_ = minValue;
      maxValue_ = maxValue;
      if (minValue == ZeroMinimum)
         textBox_.setMin(0);
      else
         textBox_.setMin(minValue);
      if (maxValue != NoMaximum)
         textBox_.setMax(maxValue);
   }

   public void setWidth(String width)
   {
      textBox_.setWidth(width);
   }

   public void setEnabled(boolean enabled)
   {
      textBox_.setEnabled(enabled);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return textBox_.addValueChangeHandler(handler);
   }

   /**
    * Make sure field is a valid integer in the range [min, max]. If min or max
    * are null, then 0 and infinity are assumed, respectively.
    */
   public boolean validate()
   {
      String value = textBox_.getValue().trim();
      if (!value.matches("^\\d+$"))
      {
         fireEvent(new EnsureVisibleEvent());
         textBox_.getElement().focus();
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Error",
               label_ + " must be a valid number.",
               textBox_);
         return false;
      }
      if (minValue_ != null || maxValue_ != null)
      {
         int intVal = Integer.parseInt(value);
         if (minValue_ != null && intVal < minValue_)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Error",
                  label_ + " must be greater than or equal to " + minValue_ + ".",
                  textBox_);
            return false;
         }
         if (maxValue_ != null && intVal > maxValue_)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Error",
                  label_ + " must be less than or equal to " + maxValue_ + ".",
                  textBox_);
            return false;
         }
      }
      return true;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }


   @Override
   public void setElementId(String id)
   {
      textBox_.getElement().setId(id);
      textBoxLabel_.setFor(id);
   }

   private final SpanLabel textBoxLabel_;
   private final NumericTextBox textBox_;
   private Integer minValue_;
   private Integer maxValue_;
   private String label_;
}
