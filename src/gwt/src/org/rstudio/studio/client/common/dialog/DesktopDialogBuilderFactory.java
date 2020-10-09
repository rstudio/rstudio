/*
 * DesktopDialogBuilderFactory.java
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;

public class DesktopDialogBuilderFactory implements DialogBuilderFactory
{
   static class Builder extends DialogBuilderBase
   {
      public Builder(int type, String caption, String message)
      {
         super(type, caption);
         message_ = message;
      }

      @Override
      public void showModal()
      {
         if (buttons_.size() == 0)
            addButton("OK", ElementIds.DIALOG_OK_BUTTON);
         
         StringBuilder buttons = new StringBuilder();
         String delim = "";
         for (ButtonSpec button : buttons_)
         {
            buttons.append(delim).append(button.label);
            delim = "|";
         }

         Desktop.getFrame().showMessageBox(
               type,
               StringUtil.notNull(caption),
               StringUtil.notNull(message_),
               StringUtil.notNull(buttons.toString()),
               defaultButton_,
               buttons_.size() - 1,
               result ->
               {
                  Builder.this.execute(String.valueOf(result));
               });
      }
      
      private void execute(String strResult)
      {
         int result = StringUtil.parseInt(strResult, -1);
         
         if (result >= buttons_.size())
            return;

         // If button has an operation, execute it in a deferred way. This
         // keeps the semantics more consistent with the web version of
         // the message dialog, which executes asynchronously.

         final ButtonSpec buttonSpec = buttons_.get(result);
         if (buttonSpec.operation != null)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  if (buttonSpec.operation != null)
                     buttonSpec.operation.execute();
               }
            });
         }
         else if (buttonSpec.progressOperation != null)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  final GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE
                        .getGlobalDisplay();

                  buttonSpec.progressOperation.execute(new ProgressIndicator()
                  {
                     public void onProgress(String message)
                     {
                        onProgress(message, null);
                     }
                     
                     public void onProgress(String message, Operation onCancel)
                     {
                        if (dismissProgress_ != null)
                           dismissProgress_.execute();
                        dismissProgress_ = globalDisplay.showProgress(message);
                     }
                     
                     public void clearProgress()
                     {
                        if (dismissProgress_ != null)
                           dismissProgress_.execute();
                     }

                     public void onCompleted()
                     {
                        if (dismissProgress_ != null)
                           dismissProgress_.execute();
                     }

                     public void onError(String message)
                     {
                        if (dismissProgress_ != null)
                           dismissProgress_.execute();

                        globalDisplay.showErrorMessage("Error", message);
                     }
                  });
               }
            });
         }
      }

      private final String message_;
      private Command dismissProgress_;
   }

   public DialogBuilder create(int type, String caption, String message)
   {
      return new Builder(type, caption, message);
   }
}
