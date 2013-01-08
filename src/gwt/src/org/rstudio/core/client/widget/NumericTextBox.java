/*
 * NumericTextBox.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.TextBox;
import org.rstudio.core.client.command.KeyboardShortcut;

public class NumericTextBox extends TextBox
{
   public NumericTextBox()
   {
      super();
      init();
   }

   protected NumericTextBox(Element element)
   {
      super(element);
      init();
   }

   private void init()
   {
      addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            selectAll();
         }
      });

      addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());
            if (modifiers == KeyboardShortcut.NONE
                && (event.isUpArrow() || event.isDownArrow()))
            {
               event.preventDefault();
               event.stopPropagation();

               try
               {
                  int value = Integer.parseInt(getText());
                  value += event.isUpArrow() ? 1 : -1;
                  setValue(value + "", true);
                  selectAll();
               }
               catch (NumberFormatException nfe)
               {
                  // just ignore
               }
            }
         }
      });

      addKeyPressHandler(new KeyPressHandler()
      {
         @Override
         public void onKeyPress(KeyPressEvent event)
         {
            char charCode = event.getCharCode();
            if (charCode >= '0' && charCode <= '9')
               return;
            if (Character.isLetterOrDigit(charCode))
               event.preventDefault();
         }
      });
   }
}
