/*
 * WebDialogBuilderFactory.java
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
package org.rstudio.studio.client.common.dialog;

import org.rstudio.core.client.DialogOptions;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.MessageDialog;

import com.google.gwt.user.client.ui.Widget;

public class WebDialogBuilderFactory implements DialogBuilderFactory
{
   static class Builder extends DialogBuilderBase
   {
      Builder(int type, String caption, Widget message, DialogOptions options)
      {
         super(type, caption);
         message_ = message;
         options_ = options;
      }

      @Override
      public void showModal()
      {
         if (buttons_.size() == 0)
            addButton("OK", ElementIds.DIALOG_OK_BUTTON);

         createDialog().showModal();
      }

      private MessageDialog createDialog()
      {
         MessageDialog messageDialog = new MessageDialog(type, caption, message_);
         for (int i = 0; i < buttons_.size(); i++)
         {
            ButtonSpec button = buttons_.get(i);
            if (button.progressOperation != null)
            {
               messageDialog.addButton(button.label,
                                       button.elementId,
                                       button.progressOperation,
                                       defaultButton_ == i,
                                       i == buttons_.size() - 1);
            }
            else
            {
               messageDialog.addButton(button.label,
                                       button.elementId,
                                       button.operation,
                                       defaultButton_ == i,
                                       i == buttons_.size() - 1);
            }
         }

         if (anchor_ != null)
            messageDialog.addLink(anchor_.label, anchor_.url);

         if (options_ != null)
         {
            if (options_.width != null)
               messageDialog.setWidth(options_.width);
            
            if (options_.height != null)
               messageDialog.setHeight(options_.height);
            
            if (options_.userSelect != null)
               messageDialog.setUserSelect(options_.userSelect);
            
         }
         return messageDialog;
      }

      private final Widget message_;
      private final DialogOptions options_;
   }

   @Override
   public DialogBuilder create(int type, String caption, String message, DialogOptions options)
   {
      return new Builder(type, caption, MessageDialog.labelForMessage(message), options);
   }

   public DialogBuilder create(int type, String caption, Widget messageWidget)
   {
      return new Builder(type, caption, messageWidget, null);
   }
   
   public DialogBuilder create(int type, String caption, Widget messageWidget, DialogOptions options)
   {
      return new Builder(type, caption, messageWidget, options);
   }
}
