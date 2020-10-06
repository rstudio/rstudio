/*
 * WebDialogBuilderFactory.java
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
package org.rstudio.studio.client.common.dialog;

import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.*;

public class WebDialogBuilderFactory implements DialogBuilderFactory
{
   static class Builder extends DialogBuilderBase
   {
      Builder(int type, String caption, Widget message)
      {
         super(type, caption);
         message_ = message;
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
         MessageDialog messageDialog = new MessageDialog(type,
                                                         caption,
                                                         message_);
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
         return messageDialog;
      }

      private final Widget message_;
   }

   @Override
   public DialogBuilder create(int type, String caption, String message)
   {
      return new Builder(type, caption, MessageDialog.labelForMessage(message));
   }

   public DialogBuilder create(int type, String caption, Widget messageWidget)
   {
      return new Builder(type, caption, messageWidget);
   }
}
