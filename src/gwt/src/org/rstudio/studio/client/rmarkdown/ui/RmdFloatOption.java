/*
 * RmdNumberOption.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.TextBox;

public class RmdFloatOption extends RmdBaseOption
{
   public RmdFloatOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option);
      HTMLPanel panel = new HTMLPanel("");
      defaultValue_ = Float.parseFloat(option.getDefaultValue());
      panel.add(new InlineLabel(option.getUiName() + ":"));
      txtValue_ = new NumericTextBox();
      txtValue_.setValue(initialValue);
      txtValue_.setWidth("40px");
      txtValue_.getElement().getStyle().setMarginLeft(5, Unit.PX);
      panel.add(txtValue_);

      initWidget(panel);
   }

   @Override
   public boolean valueIsDefault()
   {
      return defaultValue_ == Float.parseFloat(txtValue_.getText());
   }

   @Override
   public String getValue()
   {
      Float val = Float.parseFloat(txtValue_.getText());
      return val.toString();
   }
   
   private final Float defaultValue_;
   private TextBox txtValue_;
}