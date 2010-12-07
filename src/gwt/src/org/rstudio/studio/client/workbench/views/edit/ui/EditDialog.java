/*
 * EditDialog.java
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
package org.rstudio.studio.client.workbench.views.edit.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.common.reditor.REditor;
import org.rstudio.studio.client.common.reditor.model.REditorServerOperations;

public class EditDialog extends ModalDialogBase
{
   public EditDialog(String text, 
                     final ProgressOperationWithInput<String> operation,
                     EventBus events,
                     REditorServerOperations server)
   {
      server_ = server;
      setText("Edit");
      sourceText_ = text;
      events_ = events;
      
      final ProgressIndicator progressIndicator = addProgressIndicator();
      
      ThemedButton saveButton = new ThemedButton("Save", new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            operation.execute(editor_.getCode(), progressIndicator);
         }
      });
      addButton(saveButton);
      
      ThemedButton cancelButton = new ThemedButton("Cancel", new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            operation.execute(null, progressIndicator);
         }
      });
      addCancelButton(cancelButton);
      
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
   }

  
   @Override
   protected Widget createMainWidget()
   {
      // line numbers
      final boolean LINE_NUMBERS = false;
      final int LINE_NUMBERS_WIDTH = 33;
      
      // create and style editor
      editor_ = new REditor(server_);
      editor_.setCode(sourceText_);
      editor_.setLanguage(EditorLanguage.LANG_R);
      editor_.setLineNumbers(LINE_NUMBERS);
      editor_.addStyleDependentName("EditDialog");
    
      // calculate the size of the text the adjust for line numbers
      Size textSize = REditor.measureText(sourceText_);
      if (LINE_NUMBERS)
      {
         textSize = new Size(textSize.width + LINE_NUMBERS_WIDTH, 
                             textSize.height);
      }
          
      // compute the editor size
      Size minimumSize = new Size(300, 200);
      Size editorSize = DomMetrics.adjustedElementSize(textSize, 
                                                       minimumSize, 
                                                       25,   // pad
                                                       100); // client margin
     
      // set size
      editor_.setSize(editorSize.width + "px", editorSize.height + "px");
            
      // return the editor
      return editor_ ;
   }
   
   @Override
   protected void onDialogShown()
   {
      editor_.focus();
   }

   private final String sourceText_ ;
   private REditor editor_ ;
   private final EventBus events_;
   private final REditorServerOperations server_;
}
