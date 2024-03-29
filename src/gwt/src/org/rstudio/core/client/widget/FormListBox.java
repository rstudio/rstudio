/*
 * FormListBox.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.ListBox;

/**
 * ListBox with ability to set elementId (e.g. from UiBinder)
 */
public class FormListBox extends ListBox
                         implements CanSetControlId
{
   @Override
   public void setElementId(String id)
   {
      getElement().setId(id);
   }

   public int getIndexFromValue(String value)
   {
      int index = 0;
      while (index < getItemCount())
      {
         if (value == getValue(index))
            return index;
         index++;
      }
      return -1;
   }
}
