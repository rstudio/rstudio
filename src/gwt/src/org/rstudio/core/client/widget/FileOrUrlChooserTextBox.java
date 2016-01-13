/*
 * FileOrUrlChooserTextBox.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Focusable;

public class FileOrUrlChooserTextBox extends FileChooserTextBox
{

   public FileOrUrlChooserTextBox(String label, Focusable focusAfter)
   {
      super(label, focusAfter);
      
      setReadOnly(false);
   }
   
   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      // Register for programmatic changes (e.g. while choosing the browse button)
      super.addValueChangeHandler(handler);
      
      final FileChooserTextBox parent = this;
      
      // Register for user driven changes (e.g. after typing on the textbox)
      return getTextBox().addChangeHandler(new ChangeHandler()
      {
         
         @Override
         public void onChange(ChangeEvent arg0)
         {
            ValueChangeEvent.fire(parent, getText());
         }
      });
   }
   
   @Override
   public String getText()
   {
      return getTextBox().getText();
   }
}