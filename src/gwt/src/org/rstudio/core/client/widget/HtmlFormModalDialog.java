/*
 * HtmlFormModalDialog.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.DialogRole;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;


public abstract class HtmlFormModalDialog<T> extends ModalDialogBase
{
   public HtmlFormModalDialog(String title,
                              DialogRole role,
                              final String progressMessage, 
                              String actionURL,
                              final Operation beginOperation,
                              final OperationWithInput<T> completedOperation,
                              final Operation failedOperation)
   {
      super(new FormPanel(), role);
      setText(title);
      
      final FormPanel formPanel = (FormPanel)getContainerPanel();
      formPanel.getElement().getStyle().setProperty("margin", "0px");
      formPanel.getElement().getStyle().setProperty("padding", "0px");
      formPanel.setAction(actionURL);
      setFormPanelEncodingAndMethod(formPanel);
      
      final ProgressIndicator progressIndicator = addProgressIndicator();
      final ProgressIndicator indicatorWrapper = new ProgressIndicator()
      {
         public void onProgress(String message)
         {
            progressIndicator.onProgress(message);
         }

         public void onProgress(String message, Operation onCancel)
         {
            progressIndicator.onProgress(message, onCancel);
         }

         public void onCompleted()
         {
            progressIndicator.onCompleted();
         }

         public void onError(String message)
         {
            progressIndicator.onError(message);
            failedOperation.execute();
         }

         @Override
         public void clearProgress()
         {
            progressIndicator.clearProgress();
         }
      };
      
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) {
            try
            {
               formPanel.submit();
               beginOperation.execute();
            }
            catch (final JavaScriptException e)
            {
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  public void execute()
                  {
                     if ("Access is denied.".equals(
                           StringUtil.notNull(e.getDescription()).trim()))
                     {
                        indicatorWrapper.onError(
                              "Please use a complete file path.");
                     }
                     else
                     {
                        Debug.log(e.toString());
                        indicatorWrapper.onError(e.getDescription());
                     }
                  }
               });
            }
            catch (final Exception e)
            {
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  public void execute()
                  {
                     Debug.log(e.toString());
                     indicatorWrapper.onError(e.getMessage());
                  }
               });
            }
         }    
      });
      addOkButton(okButton);
      
      ThemedButton cancelButton = new ThemedButton("Cancel", new ClickHandler(){
         @Override
         public void onClick(ClickEvent event)
         {
            formPanel.clear();
            closeDialog();
         }
      });
      addCancelButton(cancelButton);
      
      
          
      formPanel.addSubmitHandler(new SubmitHandler() {
         public void onSubmit(SubmitEvent event) {           
            if (validate())
            { 
               indicatorWrapper.onProgress(progressMessage);
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
                  indicatorWrapper.onCompleted();
                  completedOperation.execute(results);
               }
               catch(Exception e)
               {
                  indicatorWrapper.onError(e.getMessage());
               }
            }
            else
            {
               indicatorWrapper.onError("Unexpected empty response from server");
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
