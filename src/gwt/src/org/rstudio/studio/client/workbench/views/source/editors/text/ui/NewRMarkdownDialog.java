/*
 * NewRMarkdownDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class NewRMarkdownDialog extends ModalDialog<NewRMarkdownDialog.Result>
{
   public static class Result
   {  
      public Result(String author, String title)
      {
         this.author = author;
         this.title = title;
      }
      
      public String toYAMLFrontMatter()
      {
         return "---\n" + 
               "author: " + author + "\n" +
               "title: " + title + "\n" +
               "---\n";
      }

      public final String author;
      public final String title;
   }

   public interface Binder extends UiBinder<Widget, NewRMarkdownDialog>
   {
   }

   public NewRMarkdownDialog(
         RMarkdownContext context,
         OperationWithInput<Result> operation)
   {
      super("New R Markdown Document", operation);
      context_ = context;
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
   }

   @Override
   protected Result collectInput()
   {
      return new Result(txtAuthor_.getText().trim(), txtTitle_.getText().trim());
   }

   @Override
   protected boolean validate(Result input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @UiField TextBox txtAuthor_;
   @UiField TextBox txtTitle_;
   
   @SuppressWarnings("unused")
   private final RMarkdownContext context_;
   
   private final Widget mainWidget_;
}
