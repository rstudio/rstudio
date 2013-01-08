/*
 * PageNumberListBox.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.pdfviewer.ui;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.ListBox;

public class PageNumberListBox extends ListBox
   implements HasValue<Integer>
{
   public PageNumberListBox()
   {
      super(false);
   }

   public void setPageCount(int value)
   {
      while (value > getItemCount())
         addItem((getItemCount() + 1) + "");

      while (value < getItemCount())
         removeItem(getItemCount() - 1);
   }

   @Override
   public Integer getValue()
   {
      if (getSelectedIndex() < 0)
         return null;
      return getSelectedIndex() + 1;
   }

   @Override
   public void setValue(Integer value)
   {
      setValue(value, true);
   }

   @Override
   public void setValue(Integer value, boolean fireEvents)
   {
      if (value == null)
         value = 0;
      Integer other = getValue();
      if (other == null)
         other = 0;

      if (other.equals(value))
         return;

      if (value < 0)
         throw new ArrayIndexOutOfBoundsException();
      if (value > getItemCount())
         setPageCount(value);

      // If value is 0 (originally null) then this will be setSelectedIndex(-1),
      // which is desired.
      setSelectedIndex(value - 1);

      if (fireEvents)
         ValueChangeEvent.fire(this, value == 0 ? null : value);
   }

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }
}
