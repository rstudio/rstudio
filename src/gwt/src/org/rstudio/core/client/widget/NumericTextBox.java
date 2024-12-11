/*
 * NumericTextBox.java
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

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextBox;

public class NumericTextBox extends TextBox
                                    implements CanSetControlId
{
   public NumericTextBox()
   {
      super();
      
      getElement().setAttribute("type", "number");
      
      addDomHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               event.stopPropagation();
               event.preventDefault();
               setFocus(false);
            }
         }
      }, KeyDownEvent.getType());
   }
   
   public NumericTextBox(int min, int max)
   {
      this();
      setMin(min);
      setMax(max);
   }
   
   public void setMin(int min)
   {
      getElement().setAttribute("min", String.valueOf(min));
   }

   public void setMax(int max)
   {
      getElement().setAttribute("max", String.valueOf(max));
   }

   @Override
   public void setElementId(String id)
   {
      getElement().setId(id);
   }
}
