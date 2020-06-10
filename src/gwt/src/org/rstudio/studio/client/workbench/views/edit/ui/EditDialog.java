/*
 * EditDialog.java
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
package org.rstudio.studio.client.workbench.views.edit.ui;

import com.google.gwt.aria.client.DialogRole;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class EditDialog extends ModalDialogBase
{
   public EditDialog(String text,
                     DialogRole role,
                     boolean isRCode,
                     boolean lineWrapping,
                     final ProgressOperationWithInput<String> operation)
   {
      this("Edit", 
           "Save",
           text, 
           role,
           isRCode, 
           lineWrapping, 
           new Size(0,0), 
           operation);
   }
   
   public EditDialog(String caption,
                     String saveCaption,
                     String text,
                     DialogRole role,
                     boolean isRCode,
                     boolean lineWrapping,
                     Size minimumSize,
                     final ProgressOperationWithInput<String> operation)
   {
      super(role);
      editor_ = new AceEditor();
      setText(caption);
      editor_.setTextInputAriaLabel(caption);
      sourceText_ = text;
      isRCode_ = isRCode;
      lineWrapping_ = lineWrapping;
      minimumSize_ = minimumSize;

      final ProgressIndicator progressIndicator = addProgressIndicator();

      ThemedButton saveButton = new ThemedButton(saveCaption, 
                                                 new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            operation.execute(editor_.getCode(), progressIndicator);
         }
      });
      addButton(saveButton, ElementIds.DIALOG_OK_BUTTON);

      ThemedButton cancelButton = new ThemedButton("Cancel", new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            operation.execute(null, progressIndicator);
         }
      });
      addCancelButton(cancelButton, ElementIds.DIALOG_CANCEL_BUTTON);

      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
   }

   @Override
   protected Widget createMainWidget()
   {
      // create widget and set size
      Widget editWidget = editor_.getWidget();
      Size size = DomMetrics.adjustedCodeElementSize(sourceText_, 25, 100);
      if (!minimumSize_.isEmpty())
      {
         size = new Size(Math.max(size.width, minimumSize_.width),
                         Math.max(size.height, minimumSize_.height));
      }
      editWidget.setSize(size.width + "px", size.height + "px");

      editor_.setCode(sourceText_, false);
      if (isRCode_)
      {
         // NOTE: line wrapping is ignored for R code since it has its
         // own localized setting for enabled/disable of line wrapping
         
         editor_.setFileType(FileTypeRegistry.R);
         
         setEscapeDisabled(true);
      }
      else
      {
         editor_.setUseWrapMode(lineWrapping_);
         editor_.setShowLineNumbers(false);
      }
      
      // return the widget
      SimplePanel panel = new SimplePanel();
      panel.addStyleName("EditDialog");
      panel.setSize(size.width + "px", size.height + "px");
      panel.setWidget(editWidget);
      return panel;
   }
   
   @Override
   protected void focusInitialControl()
   {
      editor_.focus();
   }

   private final String sourceText_;
   private final boolean isRCode_;
   private final boolean lineWrapping_;
   private final AceEditor editor_;
   private Size minimumSize_;
}
