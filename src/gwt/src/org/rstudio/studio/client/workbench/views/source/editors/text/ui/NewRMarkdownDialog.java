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
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.rmarkdown.ui.RmdTemplateOptionsWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class NewRMarkdownDialog extends ModalDialog<NewRMarkdownDialog.Result>
{
   public static class Result
   {  
      public Result(String template, String author, String title, String format, 
                    JavaScriptObject options)
      {
         template_ = template;
         author_ = author;
         result_ = toJSO(author, title, format, options);
      }
      
      public String getTemplate()
      {
         return template_;
      }
      
      public String getAuthor()
      {
         return author_;
      }
      
      public JavaScriptObject getJSOResult()
      {
         return result_;
      }
      
      private final native JavaScriptObject toJSO(String author, 
                                                 String title, 
                                                 String format, 
                                                 JavaScriptObject options) /*-{
         var output = new Object();
         output[format] = options;
         
         var result = new Object();
         if (author.length > 0)
         {
            result["author"] = author;
            result["date"] = (new Date()).toLocaleDateString();
         }
         result["title"] = title;
         result["output"] = output;
        
         return result;
      }-*/;

      private final String template_;
      private final String author_;
      private final JavaScriptObject result_;
   }

   public interface Binder extends UiBinder<Widget, NewRMarkdownDialog>
   {
   }
   
   public interface NewRmdStyle extends CssResource
   {
      // Stub to ensure style is injected during construction
   }

   public interface Resources extends ClientBundle
   {
      @Source("MarkdownPresentationIcon.png")
      ImageResource presentationIcon();

      @Source("MarkdownDocumentIcon.png")
      ImageResource documentIcon();
   }

   public NewRMarkdownDialog(
         RMarkdownContext context,
         String author,
         OperationWithInput<Result> operation)
   {
      super("New R Markdown", operation);
      context_ = context;
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      style.ensureInjected();
      txtAuthor_.setText(author);
      txtTitle_.setText("Untitled");
      listTemplates_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateOptions(getSelectedTemplate());
         }
      });

      templates_ = RmdTemplateData.getTemplates();
      for (int i = 0; i < templates_.length(); i++)
      {
         String templateName = templates_.get(i).getName();
         TemplateMenuItem menuItem = new TemplateMenuItem(templateName);
         
         ImageResource img = null;

         // Special treatment for built-in templates with known names
         if (templateName.equals(RmdTemplateData.DOCUMENT_TEMPLATE))
         {
            img = resources.documentIcon();
         } 
         else if (templateName.equals(RmdTemplateData.PRESENTATION_TEMPLATE))
         {
            img = resources.presentationIcon();
         }

         // Add an image if we have one
         if (img != null)
         {
            menuItem.addIcon(img);
         }

         listTemplates_.addItem(menuItem);
      }
      updateOptions(getSelectedTemplate());
   }
   
   @Override
   protected void onDialogShown()
   {
      // when dialog is finished booting, focus the title so it's ready to
      // accept input
      super.onDialogShown();
      txtTitle_.setSelectionRange(0, txtTitle_.getText().length());
      txtTitle_.setFocus(true);
   }

   @Override
   protected Result collectInput()
   {
      return new Result(getSelectedTemplate(), 
                        txtAuthor_.getText().trim(), 
                        txtTitle_.getText().trim(),
                        templateOptions_.getSelectedFormat(), 
                        templateOptions_.getOptionsJSON());
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
   
   private String getSelectedTemplate() 
   {
      return templates_.get(listTemplates_.getSelectedIndex()).getName();
   }
   
   private void updateOptions(String selectedTemplate)
   {
      for (int i = 0; i < templates_.length(); i++)
      {
         if (templates_.get(i).getName().equals(selectedTemplate))
         {
            templateOptions_.setTemplate(templates_.get(i));
            break;
         }
      }
   }
   
   @UiField TextBox txtAuthor_;
   @UiField TextBox txtTitle_;
   @UiField WidgetListBox listTemplates_;
   @UiField RmdTemplateOptionsWidget templateOptions_;
   @UiField NewRmdStyle style;
   @UiField Resources resources;

   private final Widget mainWidget_;

   private JsArray<RmdTemplate> templates_;
   
   @SuppressWarnings("unused")
   private final RMarkdownContext context_;
}
