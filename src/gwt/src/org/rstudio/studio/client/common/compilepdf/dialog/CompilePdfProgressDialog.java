/*
 * CompilePdfProgressDialog.java
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

package org.rstudio.studio.client.common.compilepdf.dialog;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.ProgressDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import org.rstudio.studio.client.common.compilepdf.CompilePdfErrorList;
import org.rstudio.studio.client.common.compilepdf.CompilePdfOutputBuffer;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfOutputEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfErrorsEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfError;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class CompilePdfProgressDialog extends ProgressDialog
   implements CompilePdfOutputEvent.Handler,
              CompilePdfErrorsEvent.Handler,
              CompilePdfCompletedEvent.Handler,
              HasClickHandlers,
              HasSelectionCommitHandlers<CodeNavigationTarget>
{  
   public CompilePdfProgressDialog()
   {
      super("Compiling PDF...");
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      errorList_ = new CompilePdfErrorList();
      
      addHandlerRegistration(eventBus_.addHandler(
                                    CompilePdfOutputEvent.TYPE, this));
      addHandlerRegistration(eventBus_.addHandler(
                                    CompilePdfErrorsEvent.TYPE, this));
      addHandlerRegistration(eventBus_.addHandler(
                                    CompilePdfCompletedEvent.TYPE, this));
   }
   
     
   @Inject
   void initialize(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }
   
   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return stopButton().addClickHandler(handler);
   }
   
   @Override
   public HandlerRegistration addSelectionCommitHandler(
                        SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return errorList_.addSelectionCommitHandler(handler);
   }
   
   public void dismiss()
   {
      closeDialog();
   }
   
   @Override
   protected Widget createDisplayWidget()
   {
      container_ = new SimplePanel();
      int maxHeight = Window.getClientHeight() - 150;
      int height = Math.min(500, maxHeight);
      container_.getElement().getStyle().setHeight(height, Unit.PX);
           
      output_ = new CompilePdfOutputBuffer();
      container_.setWidget(output_);
      return container_;
   }
 
   @Override
   public void onCompilePdfOutput(CompilePdfOutputEvent event)
   {
      output_.append(event.getOutput());
   }
   
   @Override
   public void onCompilePdfErrors(CompilePdfErrorsEvent event)
   {  
      errors_ = event.getErrors();
   }
   
   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      hideProgress();
      
      if (event.getSucceeded())
      {
         closeDialog();
      }
      else
      {   
         // show error list if there are errors
         String label = "Compile PDF failed";
         if (CompilePdfError.includesErrorType(errors_))
         {
            label +=  " (double-click to view source location of error)";
            errorList_.showErrors(event.getTargetFile(), errors_);
            container_.setWidget(errorList_);
         }
         
         // update the label and stop button
         setLabel(label);      
         stopButton().setText("Close");
      }
   }
   
   private EventBus eventBus_;
   
   private SimplePanel container_;
   private CompilePdfOutputBuffer output_;
   private CompilePdfErrorList errorList_;
   private JsArray<CompilePdfError> errors_;
}
