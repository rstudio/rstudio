/*
 * TextEntryModalDialog.java
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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class TextEntryModalDialog extends ModalDialog<String>
{
   public TextEntryModalDialog(String title,
                               String caption,
                               String defaultValue,
                               int selectionIndex,
                               int selectionLength, String okButtonCaption,
                               int width,
                               ProgressOperationWithInput<String> operation)
   {
      super(title, operation);
      selectionIndex_ = selectionIndex;
      selectionLength_ = selectionLength;
      width_ = width;
      textBox_ = new TextBox();
      textBox_.setWidth("100%");
      captionLabel_ = new Label(caption);
      
      if (okButtonCaption != null)
         setOkButtonCaption(okButtonCaption);
      
      if (defaultValue != null)
         textBox_.setText(defaultValue);
   }
   
   @Override
   protected void onDialogShown()
   {
      textBox_.setFocus(true);
      
      if (textBox_.getText().length() > 0)
      {
         if (selectionIndex_ >= 0 && selectionLength_ >= 0)
         {
            int offset = Math.min(selectionIndex_, textBox_.getText().length());
            int length = Math.min(selectionLength_,
                                  textBox_.getText().length() - offset);
            textBox_.setSelectionRange(offset, length);
         }
         else
            textBox_.selectAll();
      }
   }
     
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.setSpacing(6);
      verticalPanel.setWidth(width_ + "px");
      verticalPanel.add(captionLabel_);
      verticalPanel.add(textBox_);
      return verticalPanel;
   }
  
   @Override
   protected String collectInput()
   {
      return textBox_.getText();
   }

   @Override
   protected boolean validate(String input)
   {
      if (input.length() == 0)
      {
         MessageDialog dialog = new MessageDialog(MessageDialog.ERROR,
                                                  "Error",
                                                  "You must enter a value.");
         dialog.addButton("OK", (Operation)null, true, true);
         dialog.showModal();
         textBox_.setFocus(true);
         return false;
      }
      else
      {
         return true ;
      }
   }


   private int width_;
   private Label captionLabel_;
   private TextBox textBox_;
   private final int selectionIndex_;
   private final int selectionLength_;
}
