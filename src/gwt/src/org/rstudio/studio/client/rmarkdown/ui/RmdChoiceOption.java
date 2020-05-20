/*
 * RmdChoiceOption.java
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

public class RmdChoiceOption extends RmdNullableOption
{
   public RmdChoiceOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option, initialValue);
      defaultValue_ = option.getDefaultValue();

      HTMLPanel panel = new HTMLPanel("");
      choices_ = new ListBox();
      
      JsArrayString choiceList = option.getChoiceList();
      int selectedIdx = 0;
      for (int i = 0; i < choiceList.length(); i++)
      {
         choices_.addItem(choiceList.get(i));
         if (choiceList.get(i) == initialValue)
         {
            selectedIdx = i;
         }
      }
      choices_.setSelectedIndex(selectedIdx);
      panel.add(getOptionLabelWidget(choices_.getElement()));
      panel.add(choices_);

      updateNull();
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
      if (valueIsNull())
      {
         return null;
      }
      return choices_.getValue(choices_.getSelectedIndex());
   }
   
   @Override
   public void updateNull()
   {
      choices_.setEnabled(!valueIsNull());
   }
   
   private final String defaultValue_;
   private ListBox choices_;
}
