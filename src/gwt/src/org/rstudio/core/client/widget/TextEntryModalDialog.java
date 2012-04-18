/*
 * TextEntryModalDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.StringUtil;

public class TextEntryModalDialog extends ModalDialog<String>
{
   public TextEntryModalDialog(String title,
                               String caption,
                               String defaultValue,
                               boolean usePasswordMask,
                               String extraOptionPrompt,
                               boolean extraOptionDefault,
                               boolean numbersOnly,
                               int selectionIndex,
                               int selectionLength, String okButtonCaption,
                               int width,
                               ProgressOperationWithInput<String> okOperation,
                               Operation cancelOperation)
   {
      super(title, okOperation, cancelOperation);
      numbersOnly_ = numbersOnly;
      selectionIndex_ = selectionIndex;
      selectionLength_ = selectionLength;
      width_ = width;
      textBox_ = usePasswordMask ? new PasswordTextBox() :
                 numbersOnly ? new NumericTextBox() :
                 new TextBox();
      textBox_.setWidth("100%");
      captionLabel_ = new Label(caption);

      extraOption_ = new CheckBox(StringUtil.notNull(extraOptionPrompt));
      extraOption_.setVisible(
            !StringUtil.isNullOrEmpty(extraOptionPrompt));
      extraOption_.setValue(extraOptionDefault);
      
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
      verticalPanel.add(extraOption_);
      if (extraOption_.isVisible())
         verticalPanel.getElement().getStyle().setMarginBottom(10, Unit.PX);
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

      if (numbersOnly_)
      {
         try
         {
            Integer.parseInt(input.trim());
         }
         catch (NumberFormatException nfe)
         {
            MessageDialog dialog = new MessageDialog(MessageDialog.ERROR,
                                                     "Error",
                                                     "Not a valid number.");
            dialog.addButton("OK", (Operation)null, true, true);
            dialog.showModal();
            textBox_.setFocus(true);
            textBox_.selectAll();
            return false;
         }
      }

      return true ;
   }

   public boolean getExtraOption()
   {
      return extraOption_.getValue() != null
             && extraOption_.getValue();
   }


   private int width_;
   private Label captionLabel_;
   private TextBox textBox_;
   private CheckBox extraOption_;
   private final boolean numbersOnly_;
   private final int selectionIndex_;
   private final int selectionLength_;
}
