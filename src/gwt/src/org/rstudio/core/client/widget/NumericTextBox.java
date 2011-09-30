/*
 * NumericTextBox.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.TextBox;

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
