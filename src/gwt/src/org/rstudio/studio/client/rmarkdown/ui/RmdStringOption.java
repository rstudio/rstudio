/*
 * RmdStringOption.java
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

import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

public class RmdStringOption extends RmdNullableOption
{

   public RmdStringOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option, initialValue);
      defaultValue_ = option.getDefaultValue();

      HTMLPanel panel = new HTMLPanel("");
      txtValue_ = new TextBox();
      if (initialValue != "null")
         txtValue_.setValue(initialValue);
      txtValue_.getElement().getStyle().setDisplay(Display.BLOCK);
      txtValue_.getElement().getStyle().setMarginLeft(20, Unit.PX);
      txtValue_.getElement().getStyle().setMarginTop(3, Unit.PX);
      txtValue_.setWidth("75%");
      panel.add(getOptionLabelWidget(txtValue_.getElement()));
      panel.add(txtValue_);

      updateNull();
      initWidget(panel);
   }
   
   @Override
   public boolean valueIsDefault()
   {
      if (getValue() == null)
         return defaultValue_ == "null";
      else 
         return defaultValue_ == txtValue_.getValue();
   }

   @Override
   public String getValue()
   {
      if (valueIsNull())
         return null;
      else if (getOption().isNullable() && txtValue_.getValue().trim().isEmpty())
         return null;
      else
         return txtValue_.getValue().trim();
   }

   @Override
   public void updateNull()
   {
      txtValue_.setEnabled(!valueIsNull());
   }

   private TextBox txtValue_;
   
   private String defaultValue_;
}
