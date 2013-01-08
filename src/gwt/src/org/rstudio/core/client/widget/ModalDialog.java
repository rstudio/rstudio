/*
 * ModalDialog.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;


public abstract class ModalDialog<T> extends ModalDialogBase
{
   public ModalDialog(String caption, 
                      OperationWithInput<T> operation)
   {
      this(caption, operation, null);
   }
   
   public ModalDialog(String caption, 
                      final OperationWithInput<T> operation,
                      Operation cancelOperation)
   {
      super();
    
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) {
            final T input = collectInput();
            validateAndGo(input, new Command()
            {
               @Override
               public void execute()
               {
                  closeDialog();
                  if (operation != null)
                     operation.execute(input);
                  onSuccess();
               }
            });
         }
      });
      
      commonInit(caption, okButton, cancelOperation);
   }

   protected void onSuccess()
   {
   }


   public ModalDialog(String caption,
                      final ProgressOperationWithInput<T> operation)
   {
      this(caption, operation, null);
   }

   public ModalDialog(String caption, 
                      final ProgressOperationWithInput<T> operation,
                      Operation cancelOperation)
   {
      super();

      final ProgressIndicator progressIndicator = addProgressIndicator();
      
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) {
            final T input = collectInput();
            validateAndGo(input, new Command()
            {
               @Override
               public void execute()
               {
                  operation.execute(input, progressIndicator);
                  onSuccess();
               }
            });
         }
      });
      
      commonInit(caption, okButton, cancelOperation);
   }
   
   private void commonInit(String caption,
                           ThemedButton okButton,
                           final Operation cancelOperation)
   {
      setText(caption);
      addOkButton(okButton);
      ThemedButton cancelButton = addCancelButton();
      if (cancelOperation != null)
      {
         cancelButton.addClickHandler(new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               cancelOperation.execute();
            }
         });
      }
   }
   
   protected abstract T collectInput();

   protected void validateAndGo(T input, Command executeOnSuccess)
   {
      if (validate(input))
         executeOnSuccess.execute();
   }

   protected abstract boolean validate(T input);
  
}
