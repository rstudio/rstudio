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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RmdChosenTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.ui.RmdTemplateChooser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class NewRMarkdownDialog extends ModalDialog<NewRMarkdownDialog.Result>
{
   public static class RmdNewDocument
   {  
      public RmdNewDocument(String template, String author, String title, 
                            String format)
      {
         template_ = template;
         author_ = author;
         result_ = toJSO(author, title, format);
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
      
      private final JavaScriptObject toJSO(String author, 
                                           String title, 
                                           String format)
      {
         RmdFrontMatter result = RmdFrontMatter.create(title);
         if (author.length() > 0)
         {
            result.setAuthor(author);
            result.addDate();
         }
         result.setOutputOption(format, RmdFrontMatterOutputOptions.create());
         return result;
      }

      private final String template_;
      private final String author_;
      private final JavaScriptObject result_;
   }
   
   public static class Result
   {
      public Result (RmdNewDocument newDocument, RmdChosenTemplate fromTemplate,
                     boolean isNewDocument)
      {
         newDocument_ = newDocument;
         fromTemplate_ = fromTemplate;
         isNewDocument_ = isNewDocument;
      }
      
      public RmdNewDocument getNewDocument()
      {
         return newDocument_;
      }
      
      public RmdChosenTemplate getFromTemplate()
      {
         return fromTemplate_;
      }
      
      public boolean isNewDocument()
      {
         return isNewDocument_;
      }
      
      private final RmdNewDocument newDocument_;
      private final RmdChosenTemplate fromTemplate_;
      private final boolean isNewDocument_;
   }

   public interface Binder extends UiBinder<Widget, NewRMarkdownDialog>
   {
   }
   
   public interface NewRmdStyle extends CssResource
   {
      String outputFormat();
      String outputFormatName();
      String outputFormatChoice();
      String outputFormatDetails();
      String outputFormatIcon();
   }

   public interface Resources extends ClientBundle
   {
      @Source("MarkdownPresentationIcon.png")
      ImageResource presentationIcon();

      @Source("MarkdownDocumentIcon.png")
      ImageResource documentIcon();

      @Source("MarkdownOptionsIcon.png")
      ImageResource optionsIcon();
   }

   public NewRMarkdownDialog(
         RMarkdownContext context,
         String author,
         OperationWithInput<Result> operation)
   {
      super("New R Markdown", operation);
      context_ = context;
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      formatOptions_ = new ArrayList<RadioButton>();
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
      
      listTemplates_.addItem(new TemplateMenuItem(TEMPLATE_CHOOSE_EXISTING));
      
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
      String formatName = "";
      for (int i = 0; i < formatOptions_.size(); i++)
      {
         if (formatOptions_.get(i).getValue())
         {
            formatName = currentTemplate_.getFormats().get(i).getName();
               break;
         }
      }
      return new Result(
            new RmdNewDocument(getSelectedTemplate(), 
                               txtAuthor_.getText().trim(), 
                                txtTitle_.getText().trim(), formatName),
            templateChooser_.getChosenTemplate(),
            !getSelectedTemplate().equals(TEMPLATE_CHOOSE_EXISTING));
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
      int idx = listTemplates_.getSelectedIndex();
      TemplateMenuItem item = listTemplates_.getItemAtIdx(idx);
      if (item.getName() == TEMPLATE_CHOOSE_EXISTING)
         return TEMPLATE_CHOOSE_EXISTING;
      else
         return templates_.get(idx).getName();
   }
   
   private void updateOptions(String selectedTemplate)
   {
      boolean existing = selectedTemplate.equals(TEMPLATE_CHOOSE_EXISTING);

      newTemplatePanel_.setVisible(!existing);
      existingTemplatePanel_.setVisible(existing);
      
      if (existing)
      {
         templateChooser_.populateTemplates();
         return;
      }

      currentTemplate_ = RmdTemplate.getTemplate(templates_,
                                                 selectedTemplate);
      if (currentTemplate_ == null)
         return;
      
      templateFormatPanel_.clear();
      formatOptions_.clear();
      
      // Add each format to the dialog
      JsArray<RmdTemplateFormat> formats = currentTemplate_.getFormats();
      for (int i = 0; i < formats.length(); i++)
      {
         templateFormatPanel_.add(createFormatOption(formats.get(i)));
      }
      
      // Select the first format by default
      if (formatOptions_.size() > 0)
         formatOptions_.get(0).setValue(true);
   }
   
   private Widget createFormatOption(RmdTemplateFormat format)
   {
      HTMLPanel formatWrapper = new HTMLPanel("");
      formatWrapper.setStyleName(style.outputFormat());
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.appendHtmlConstant("<span class=\"" + style.outputFormatName() + 
                            "\">");
      sb.appendEscaped(format.getUiName());
      sb.appendHtmlConstant("</span>");
      RadioButton button = new RadioButton("DefaultOutputFormat", 
                                           sb.toSafeHtml().asString(), true);
      button.setStyleName(style.outputFormatChoice());
      formatOptions_.add(button);
      formatWrapper.add(button);
      Label label = new Label(format.getNotes());
      label.setStyleName(style.outputFormatDetails());
      formatWrapper.add(label);
      return formatWrapper;
   }
   
   @UiField TextBox txtAuthor_;
   @UiField TextBox txtTitle_;
   @UiField WidgetListBox<TemplateMenuItem> listTemplates_;
   @UiField NewRmdStyle style;
   @UiField Resources resources;
   @UiField HTMLPanel templateFormatPanel_;
   @UiField HTMLPanel newTemplatePanel_;
   @UiField HTMLPanel existingTemplatePanel_;
   @UiField RmdTemplateChooser templateChooser_;

   private final Widget mainWidget_;
   private List<RadioButton> formatOptions_;
   private JsArray<RmdTemplate> templates_;
   private RmdTemplate currentTemplate_;

   @SuppressWarnings("unused")
   private final RMarkdownContext context_;
   
   private final static String TEMPLATE_CHOOSE_EXISTING = "From Template";
}
