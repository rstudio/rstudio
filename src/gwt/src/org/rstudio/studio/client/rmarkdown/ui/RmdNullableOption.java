/*
 * RmdNullableOption.java
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

import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class RmdNullableOption extends RmdBaseOption
{
   public RmdNullableOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option);
      if (option.isNullable())
      {
         nonNullCheck_ = new CheckBox(option.getUiName() + ": ");
         nonNullCheck_.setValue(initialValue != "null");
         nonNullCheck_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
         {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event)
            {
               updateNull();
            }
         });
      }
      else
      {
         notNullableLabel_ = new FormLabel(true, getOption().getUiName() + ": ", FormLabel.NoForId);
      }
   }

   public abstract void updateNull();
   
   protected boolean valueIsNull()
   {
      return nonNullCheck_ != null && !nonNullCheck_.getValue();
   }

   /**
    * Associates label with an element and returns it
    * @param ele element referred to by the label
    * @return label
    */
   protected Widget getOptionLabelWidget(Element ele)
   {
      if (nonNullCheck_ != null)
      {
         // visible "label" is the checkbox, apply it's text as the aria-label on the
         // control associated with the checkbox
         ele.setAttribute("aria-label", nonNullCheck_.getText());
         return nonNullCheck_;
      }
      else
      {
         // visible label is the label for this control
         notNullableLabel_.setFor(ele);
         return notNullableLabel_;
      }
   }

   private CheckBox nonNullCheck_;
   private FormLabel notNullableLabel_;
}
