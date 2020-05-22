/*
 * RmdBooleanOption.java
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

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;

public class RmdBooleanOption extends RmdBaseOption
{
   public RmdBooleanOption(RmdTemplateFormatOption option, String initialValue)
   {
      super(option);
      HTMLPanel panel = new HTMLPanel("");
      checkBox_ = new CheckBox(option.getUiName());
      defaultValue_ = Boolean.parseBoolean(option.getDefaultValue());
      checkBox_.setValue(Boolean.parseBoolean(initialValue));
      panel.add(checkBox_);
      initWidget(panel);
   }

   @Override
   public boolean valueIsDefault()
   {
      return defaultValue_ == checkBox_.getValue();
   }

   @Override
   public String getValue()
   {
      return checkBox_.getValue().toString();
   }
   
   private boolean defaultValue_;
   private CheckBox checkBox_;
}
