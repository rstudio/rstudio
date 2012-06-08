/*
 * CompileNotebookOptionsDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.notebook;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;

public class CompileNotebookOptionsDialog extends ModalDialog<CompileNotebookOptions>
{
   interface Binder extends UiBinder<Widget, CompileNotebookOptionsDialog>
   {}

   public CompileNotebookOptionsDialog(
         String docId,
         String defaultTitle,
         String defaultAuthor,
         final OperationWithInput<CompileNotebookOptions> operation)
   {
      super("Compile Notebook", operation);
      docId_ = docId;

      widget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      txtTitle_.setText(defaultTitle);
      txtAuthor_.setText(defaultAuthor);
   }

   @Override
   protected CompileNotebookOptions collectInput()
   {
      return CompileNotebookOptions.create(docId_,
                                           createPrefix(),
                                           createSuffix(),
                                           true,
                                           txtTitle_.getValue().trim(),
                                           txtAuthor_.getValue().trim());
   }

   private String createPrefix()
   {
      StringBuilder builder = new StringBuilder();
      String title = txtTitle_.getValue().trim();
      if (title.length() > 0)
      {
         builder.append("# ")
                .append(SafeHtmlUtils.htmlEscape(title))
                .append("\n");
      }

      String author = txtAuthor_.getValue().trim();
      if (author.length() > 0)
      {
         builder.append("### By ")
                .append(SafeHtmlUtils.htmlEscape(author))
                .append("\n");
      }

      return builder.toString();
   }

   private String createSuffix()
   {
      return "";
   }

   @Override
   protected boolean validate(CompileNotebookOptions input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }

   private final String docId_;

   @UiField
   TextBox txtTitle_;
   @UiField
   TextBox txtAuthor_;

   private Widget widget_;
}
