/*
 * NumericTextBox.java
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

import com.google.gwt.user.client.ui.TextBox;

public class NumericTextBox extends TextBox
                                    implements CanSetControlId
{
   public NumericTextBox()
   {
      super();
      getElement().setAttribute("type", "number");
   }

   public void setMax(int max)
   {
      getElement().setAttribute("max", String.valueOf(max));
   }

   public void setMin(int min)
   {
      getElement().setAttribute("min", String.valueOf(min));
   }

   @Override
   public void setElementId(String id)
   {
      getElement().setId(id);
   }
}
