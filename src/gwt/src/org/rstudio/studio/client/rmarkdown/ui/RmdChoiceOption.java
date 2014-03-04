/*
 * RmdChoiceOption.java
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ListBox;

public class RmdChoiceOption extends RmdBaseOption
{
   public RmdChoiceOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option);
      defaultValue_ = option.getDefaultValue();

      HTMLPanel panel = new HTMLPanel("");
      if (option.isNullable())
      {
         nonNullCheck_ = new CheckBox();
         nonNullCheck_.setValue(!initialValue.equals("null"));
         panel.add(nonNullCheck_);
      }
      panel.add(new InlineLabel(option.getUiName() + ":"));
      choices_ = new ListBox();
      
      JsArrayString choiceList = option.getChoiceList();
      int selectedIdx = 0;
      for (int i = 0; i < choiceList.length(); i++)
      {
         choices_.addItem(choiceList.get(i));
         if (choiceList.get(i).equals(initialValue))
         {
            selectedIdx = i;
         }
      }
      choices_.setSelectedIndex(selectedIdx);
      panel.add(choices_);

      if (option.isNullable())
      {
         nonNullCheck_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
         {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event)
            {
               updateNull();
            }
         });
         updateNull();
      }

      initWidget(panel);
   }

   @Override
   public boolean valueIsDefault()
   {
      return defaultValue_ == getValue();
   }

   @Override
   public String getValue()
   {
      if (nonNullCheck_ != null && !nonNullCheck_.getValue())
      {
         return null;
      }
      return choices_.getValue(choices_.getSelectedIndex());
   }
   
   private void updateNull()
   {
      choices_.setEnabled(nonNullCheck_.getValue());
   }
   
   private final String defaultValue_;
   private ListBox choices_;
   private CheckBox nonNullCheck_;
}