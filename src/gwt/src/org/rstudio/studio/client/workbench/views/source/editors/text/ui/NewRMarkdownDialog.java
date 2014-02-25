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

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class NewRMarkdownDialog extends ModalDialog<NewRMarkdownDialog.Result>
{
   public static class Result
   {  
      public Result(String author, String title, String format)
      {
         this.author = author;
         this.title = title;
         this.format = format;
      }
      
      public String toYAMLFrontMatter()
      {
         return "---\n" + 
               "author: " + author + "\n" +
               "title: " + title + "\n" +
               "format: " + format + "\n" + 
               "---\n";
      }

      public final String author;
      public final String title;
      public final String format;
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
         listTemplates_.addItem(templates_.get(i).getName());
      }
      listTemplates_.setSelectedIndex(0);
      updateOptions(getSelectedTemplate());
   }

   @Override
   protected Result collectInput()
   {
      return new Result(txtAuthor_.getText().trim(), txtTitle_.getText().trim(),
            getSelectedFormat());
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
      return listTemplates_.getItemText(listTemplates_.getSelectedIndex());
   }
   
   private String getSelectedFormat()
   {
      return listFormats_.getValue(listTemplates_.getSelectedIndex());
   }
   
   private void updateOptions(String selectedTemplate)
   {
      for (int i = 0; i < templates_.length(); i++)
      {
         if (templates_.get(i).getName().equals(selectedTemplate))
         {
            updateOptions(templates_.get(i));
            break;
         }
      }
   }
   
   private void updateOptions(RmdTemplate template)
   {
      panelOptions_.clear();
      formats_ = template.getFormats();
      options_ = template.getOptions();
      panelOptions_.add(new Label("Format: "));
      listFormats_ = new ListBox();
      listFormats_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateFormatOptions(getSelectedFormat());
         }
      });
      panelOptions_.add(listFormats_);
      for (int i = 0; i < formats_.length(); i++)
      {
         listFormats_.addItem(formats_.get(i).getUiName(), 
                              formats_.get(i).getName());
      }
      mapOptions_ = new HashMap<String, RmdTemplateFormatOption>();
      for (int i = 0; i < options_.length(); i++)
      {
         mapOptions_.put(options_.get(i).getName(), options_.get(i));
      }
   }
   
   private void updateFormatOptions(String format)
   {
      for (int i = 0; i < formats_.length(); i++)
      {
         if (formats_.get(i).getName().equals(format))
         {
            updateFormatOptions(formats_.get(i));
            break;
         }
      }
   }
   
   private void updateFormatOptions(RmdTemplateFormat format)
   {
   }
   
   @UiField TextBox txtAuthor_;
   @UiField TextBox txtTitle_;
   @UiField ListBox listTemplates_;
   @UiField StackPanel panelOptions_;
   
   private ListBox listFormats_;
   private final Widget mainWidget_;

   private JsArray<RmdTemplate> templates_;
   private JsArray<RmdTemplateFormat> formats_;
   private JsArray<RmdTemplateFormatOption> options_;
   private Map<String, RmdTemplateFormatOption> mapOptions_;
   
   @SuppressWarnings("unused")
   private final RMarkdownContext context_;
}
