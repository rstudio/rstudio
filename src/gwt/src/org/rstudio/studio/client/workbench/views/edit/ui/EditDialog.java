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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class EditDialog extends ModalDialogBase
{
   public EditDialog(String text,
                     boolean isRCode,
                     final ProgressOperationWithInput<String> operation)
   {
      editor_ = new AceEditor();
      setText("Edit");
      sourceText_ = text;
      isRCode_ = isRCode;

      setEscapeDisabled(true);

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
      final int LINE_NUMBERS_WIDTH = 100;

      // calculate the size of the text the adjust for line numbers
      Size textSize = DomMetrics.measureCode(sourceText_);
      textSize = new Size(textSize.width + LINE_NUMBERS_WIDTH,
                          textSize.height);

      // compute the editor size
      Size minimumSize = new Size(300, 200);
      Size editorSize = DomMetrics.adjustedElementSize(textSize,
                                                       minimumSize,
                                                       25,   // pad
                                                       100); // client margin

      // set size
      Widget editWidget = editor_.getWidget();
      editWidget.setSize(editorSize.width + "px", editorSize.height + "px");

      editor_.setCode(sourceText_, false);
      if (isRCode_)
      {
         editor_.setFileType(FileTypeRegistry.R);
      }
      else
      {
         editor_.setShowLineNumbers(false);
      }
     
      // return the editor
      SimplePanel panel = new SimplePanel();
      panel.addStyleName("EditDialog");
      panel.setSize(editorSize.width + "px", editorSize.height + "px");
      panel.setWidget(editWidget);
      return panel;
   }
   
   @Override
   protected void onDialogShown()
   {
      editor_.focus();
   }

   private final String sourceText_ ;
   private final boolean isRCode_;
   private AceEditor editor_ ;
}
