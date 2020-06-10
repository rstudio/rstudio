/*
 * RmdNumberOption.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

public class RmdFloatOption extends RmdNullableOption
{
   public RmdFloatOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option, initialValue);
      HTMLPanel panel = new HTMLPanel("");
      defaultValue_ = Float.parseFloat(option.getDefaultValue());
      txtValue_ = new NumericTextBox();
      if (initialValue.equals("null"))
         txtValue_.setValue(option.getDefaultValue());
      else
         txtValue_.setValue(initialValue);
      txtValue_.setWidth("40px");
      txtValue_.getElement().getStyle().setMarginLeft(5, Unit.PX);
      panel.add(getOptionLabelWidget(txtValue_.getElement()));
      panel.add(txtValue_);

      updateNull();

      initWidget(panel);
   }

   @SuppressWarnings("unlikely-arg-type")
   @Override
   public boolean valueIsDefault()
   {
      if (valueIsNull())
         return defaultValue_.equals("null");
      return defaultValue_ == Float.parseFloat(txtValue_.getText());
   }

   @Override
   public String getValue()
   {
      if (valueIsNull())
         return null;
      Float val = Float.parseFloat(txtValue_.getText());
      return val.toString();
   }

   @Override
   public void updateNull()
   {
      txtValue_.setEnabled(!valueIsNull());
   }
   
   private final Float defaultValue_;
   private TextBox txtValue_;

}
