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
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.reditor.REditor;
import org.rstudio.studio.client.common.reditor.model.REditorServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

public class EditDialog extends ModalDialogBase
{
   public static void edit(
         final String text,
         final ProgressOperationWithInput<String> operation,
         final EventBus events,
         final REditorServerOperations server)
   {
      AceEditor.create(new CommandWithArg<AceEditor>()
      {
         public void execute(AceEditor editor)
         {
            new EditDialog(text,
                           operation,
                           events,
                           server,
                           editor).showModal();
         }
      });
   }

   private EditDialog(String text,
                      final ProgressOperationWithInput<String> operation,
                      EventBus events,
                      REditorServerOperations server,
                      AceEditor editor)
   {
      server_ = server;
      editor_ = editor;
      setText("Edit");
      sourceText_ = text;
      events_ = events;

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
      Size textSize = REditor.measureText(sourceText_);
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

      editor_.setCode(sourceText_);
      editor_.setFileType(FileTypeRegistry.R);

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
   private AceEditor editor_ ;
   private final EventBus events_;
   private final REditorServerOperations server_;
}
