/*
 * HtmlFormModalDialog.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;


public abstract class HtmlFormModalDialog<T> extends ModalDialogBase
{
   public HtmlFormModalDialog(String title, 
                              final String progressMessage, 
                              String actionURL,
                              final OperationWithInput<T> operation)
   {
      super(new FormPanel());
      setText(title);
      
      final FormPanel formPanel = (FormPanel)getContainerPanel();
      formPanel.getElement().getStyle().setProperty("margin", "0px");
      formPanel.getElement().getStyle().setProperty("padding", "0px");
      formPanel.setAction(actionURL);
      setFormPanelEncodingAndMethod(formPanel);
      
      final ProgressIndicator progressIndicator = addProgressIndicator();
      
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) {
            formPanel.submit();
         }    
      });
      addOkButton(okButton);
      addCancelButton();
          
      formPanel.addSubmitHandler(new SubmitHandler() {
         public void onSubmit(SubmitEvent event) {           
            if (validate())
            { 
               progressIndicator.onProgress(progressMessage);
            }
            else
            {
               event.cancel();
            }
         }
      });
      
      formPanel.addSubmitCompleteHandler(new SubmitCompleteHandler() {
         public void onSubmitComplete(SubmitCompleteEvent event) {
            
            String resultsText = event.getResults();
            if (resultsText != null)
            {
               try
               {
                  T results = parseResults(resultsText);
                  progressIndicator.onCompleted();
                  operation.execute(results);
               }
               catch(Exception e)
               {
                  progressIndicator.onError(e.getMessage());
               }
            }
            else
            {
               progressIndicator.onError(
                                    "Unexpected empty response from server");
            }      
         }
      });
   }   
   
   protected void setFormPanelEncodingAndMethod(FormPanel formPanel)
   {
      formPanel.setEncoding(FormPanel.ENCODING_URLENCODED);
      formPanel.setMethod(FormPanel.METHOD_POST);
   }
   
   protected abstract boolean validate();
   protected abstract T parseResults(String results) throws Exception;
}
