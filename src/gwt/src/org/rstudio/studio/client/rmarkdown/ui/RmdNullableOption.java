/*
 * RmdNullableOption.java
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

import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

public abstract class RmdNullableOption extends RmdBaseOption
{
   public RmdNullableOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option);
      if (option.isNullable())
      {
         nonNullCheck_ = new CheckBox(option.getUiName() + ": ");
         nonNullCheck_.setValue(!initialValue.equals("null"));
         nonNullCheck_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
         {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event)
            {
               updateNull();
            }
         });
      }
   }

   public abstract void updateNull();
   
   protected boolean valueIsNull()
   {
      return nonNullCheck_ != null && !nonNullCheck_.getValue();
   }
   
   protected Widget getOptionLabelWidget()
   {
      if (nonNullCheck_ != null)
         return nonNullCheck_;
      else
         return new InlineLabel(getOption().getUiName() + ": ");
   }
   
   private CheckBox nonNullCheck_;
}
