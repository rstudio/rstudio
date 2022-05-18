/*
 * NewQuartoDocumentDialog.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.quarto.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.newdocument.NewDocumentResources;
import org.rstudio.studio.client.common.newdocument.TemplateMenuItem;
import org.rstudio.studio.client.quarto.QuartoConstants;
import org.rstudio.studio.client.quarto.model.QuartoCapabilities;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.quarto.model.QuartoCommandConstants;
import org.rstudio.studio.client.quarto.model.QuartoJupyterKernel;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;
import com.google.inject.Inject;

public class NewQuartoDocumentDialog extends ModalDialog<NewQuartoDocumentDialog.Result>
{
   public static class Result extends JavaScriptObject
   {   
      protected Result() {}

      public static final Result createDefault()
      {
         return create("", "", 
                      QuartoCommandConstants.FORMAT_HTML,
                      QuartoCommandConstants.ENGINE_KNITR,
                      "python3", 
                      "python", 
                      QuartoCommandConstants.EDITOR_VISUAL,
                      false);
      }

      public static final native Result create(String title,
                                               String author,
                                               String format,
                                               String engine,
                                               String kernel,
                                               String language,
                                               String editor,
                                               boolean empty)
      /*-{
         return {
            "title": title,
            "author": author,
            "format": format,
            "engine": engine,
            "kernel": kernel,
            "language": language,
            "editor": editor,
            "empty": empty
         };
      }-*/;
      
      public final native Result clone() 
         /*-{
         return {
            "title": this.title,
            "author": this.author,
            "format": this.format,
            "engine": this.engine,
            "kernel": this.kernel,
            "language": this.language,
            "editor": this.editor,
            "empty": this.empty
         };
      }-*/;
      

      public final native String getTitle() /*-{ return this["title"]; }-*/;
      public final native String getAuthor() /*-{ return this["author"]; }-*/;
      public final native String getFormat() /*-{ return this["format"]; }-*/;
      public final native String getEngine() /*-{ return this["engine"]; }-*/;
      public final native String getKernel() /*-{ return this["kernel"]; }-*/;
      public final native String getLanguage() /*-{ return this["language"]; }-*/;
      public final native String getEditor() /*-{ return this["editor"]; }-*/;
      public final native boolean getEmpty() /*-{ return !!this["empty"] }-*/;
   }
   

   public interface Binder extends UiBinder<Widget, NewQuartoDocumentDialog>
   {
   }

   public NewQuartoDocumentDialog(QuartoCapabilities caps, 
                                  boolean presentation,
                                  OperationWithInput<Result> operation)
   {
      super(constants_.newQuartoDocumentCaption(), Roles.getDialogRole(), operation);
      RStudioGinjector.INSTANCE.injectMembers(this);

      loadAndPersistClientState();
      
      setOkButtonCaption(constants_.createDocButtonCaption());
      
      caps_ = caps;

      mainWidget_ = GWT.<Binder> create(Binder.class).createAndBindUi(this);
      
      formatOptions_ = new ArrayList<>();
      formatNames_ = new ArrayList<>();
      resources.styles().ensureInjected();
      
      txtTitle_.setText("Untitled"); //$NON-NLS-1$
      DomUtils.setPlaceholder(txtAuthor_, constants_.newDocAuthorPlaceholderText());
      Roles.getListboxRole().setAriaLabelProperty(listTemplates_.getElement(), constants_.templateAriaLabelValue());
      listTemplates_.addChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateOptions(getSelectedTemplate());
         }
      });

      TemplateMenuItem docTemplate = new TemplateMenuItem(TEMPLATE_DOCUMENT);
      docTemplate.addIcon(new ImageResource2x(resources.documentIcon2x()));
      listTemplates_.addItem(docTemplate);

      TemplateMenuItem presTemplate = new TemplateMenuItem(TEMPLATE_PRESENTATION);
      presTemplate.addIcon(new ImageResource2x(resources.presentationIcon2x()));
      listTemplates_.addItem(presTemplate);

      TemplateMenuItem appTemplate = new TemplateMenuItem(TEMPLATE_INTERACTIVE);
      appTemplate.addIcon(new ImageResource2x(resources.shinyIcon2x()));
      listTemplates_.addItem(appTemplate);
      
      if (presentation)
         listTemplates_.setSelectedIndex(1);
      
      Label engineLabel = createLabel(constants_.engineLabelCaption());
      engineSelect_ = createListBox(
         new String[] {constants_.engineSelectNoneLabel(), "Knitr", "Jupyter"},
         new String[] {
           QuartoCommandConstants.ENGINE_MARKDOWN,
           QuartoCommandConstants.ENGINE_KNITR,
           QuartoCommandConstants.ENGINE_JUPYTER
         }
      );
      setListBoxValue(engineSelect_, lastResult_.getEngine());
            
      Label kernelLabel = createLabel(constants_.kernelLabelCaption());
      JsArray<QuartoJupyterKernel> kernels = caps.jupyterKernels();
      
      String[] kernelNames = new String[kernels.length()];
      String[] kernelDisplayNames = new String[kernels.length()];
      for (int i=0; i<kernels.length(); i++)
      {
         QuartoJupyterKernel kernel = kernels.get(i);
         kernelNames[i] = kernel.getName();
         kernelDisplayNames[i] = kernel.getDisplayName();
      }
      kernelSelect_ = createListBox(kernelDisplayNames, kernelNames);
      setListBoxValue(kernelSelect_, lastResult_.getKernel());
      
      Label editorLabel = createLabel(constants_.editorText());
      editorCheckBox_ = new QuartoVisualEditorCheckBox();
      
      // use project default if available, otherwise use last result
      QuartoConfig config = session_.getSessionInfo().getQuartoConfig();
      String editor = config.project_editor != null ? config.project_editor.mode : null;
      if (StringUtil.isNullOrEmpty(editor))
         editor = lastResult_.getEditor();
      editorCheckBox_.setValue(editor.equals(QuartoCommandConstants.EDITOR_VISUAL));
      
      // Add them to parent
      grid_.getElement().getStyle().setMarginTop(4, Unit.PX);
      grid_.resize(3, 2);
      grid_.setCellPadding(2);
      
      grid_.addStyleName(RES.styles().grid());
      
      grid_.setWidget(ROW_ENGINE, 0, engineLabel);
      grid_.setWidget(ROW_ENGINE, 1, engineSelect_);
      
      grid_.setWidget(ROW_KERNEL, 0, kernelLabel);
      grid_.setWidget(ROW_KERNEL, 1, kernelSelect_);
      
      grid_.setWidget(ROW_EDITOR, 0, editorLabel);
      grid_.setWidget(ROW_EDITOR, 1, editorCheckBox_);
      
     
      // tweak some row spacing
      RowFormatter rowFmt = grid_.getRowFormatter();
      rowFmt.addStyleName(ROW_ENGINE, RES.styles().spacedRow());
      rowFmt.addStyleName(ROW_EDITOR, RES.styles().spacedRow());
      
      quartoHelpLink_ = addHelpLink(constants_.learnMoreLinkCaption(),
                                    "https://quarto.org");
      quartoPresentationsHelpLink_ = addHelpLink(
         constants_.learnMorePresentationsLinkCaption(),
         "https:/quarto.org/docs/presentations/");
      quartoInteractiveHelpLink_ = addHelpLink(
         constants_.learnMoreInteractiveDocsLinkCaption(),
         "https://quarto.org/docs/interactive/");
      
      updateOptions(getSelectedTemplate());
            
      // Add option to create empty document
      ThemedButton emptyDoc = new ThemedButton(constants_.createEmptyDocButtonTitle(), evt -> {
         closeDialog();
         if (operation != null)
            operation.execute(getResult(true));
         onSuccess();
      });
      addLeftButton(emptyDoc, ElementIds.EMPTY_DOC_BUTTON);
   }
   
   @Inject
   private void initialize(Session session, GlobalDisplay globalDisplay)
   {
      session_ = session;
      globalDisplay_ = globalDisplay;
   }


   @Override
   protected void focusInitialControl()
   {
      txtTitle_.setFocus(true);
      txtTitle_.selectAll();
   }
   

   @Override
   protected Result collectInput()
   {   
      lastResult_ = getResult(false);  
      return lastResult_.clone();
   }
   
   private Result getResult(boolean empty)
   {
      String formatName = getSelectedFormat();
      
      String engine = engineSelect_.getSelectedValue();
      String kernel = kernelSelect_.getSelectedValue();
      String editor = editorCheckBox_.getValue() 
                         ? QuartoCommandConstants.EDITOR_VISUAL :
                           QuartoCommandConstants.EDITOR_SOURCE;
      
      // determine language
      String language = null;
      if (engine.equals("knitr"))
      {
         language = "r";
      }
      else
      {
         JsArray<QuartoJupyterKernel> kernels = caps_.jupyterKernels();
         for (int i=0; i<kernels.length(); i++)
         {
            if (kernels.get(i).getName().equals(kernel))
            {
               language = kernels.get(i).getLanguage();
               break;
            }
         }
      }
      
      return Result.create(
         txtTitle_.getText().trim(),
         txtAuthor_.getText().trim(),
         formatName,
         engine,
         kernel,
         language,
         editor,
         empty
       );
   }
   
   @Override
   protected boolean validate(Result result)
   {
      String title = result.getTitle();
      if (StringUtil.isNullOrEmpty(title))
      {
         globalDisplay_.showErrorMessage(
            constants_.titleRequiredErrorCaption(),
            constants_.titleRequiredErrorMessage(),
            txtTitle_);
         return false;
      }
      
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
      return item.getName();
   }
   
   private String getSelectedFormat()
   {
      String formatName = null;
      for (int i = 0; i < formatOptions_.size(); i++)
      {
         if (formatOptions_.get(i).getValue())
         {
            formatName = formatNames_.get(i);
            break;
         }
      }
      return formatName;
   }

   private void updateOptions(String selectedTemplate)
   {
     
      templateFormatPanel_.clear();
      formatNames_.clear();
      formatOptions_.clear();
      quartoHelpLink_.setVisible(false);
      quartoPresentationsHelpLink_.setVisible(false);
      quartoInteractiveHelpLink_.setVisible(false);

      if (selectedTemplate.equals(TEMPLATE_DOCUMENT))
      {
         quartoHelpLink_.setVisible(true);
         
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.FORMAT_HTML,
            constants_.htmlFormatText(),
            constants_.htmlFormatDesc())
         );
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.FORMAT_PDF,
            constants_.pdfFormatText(),
            constants_.pdfFormatDesc())
         );
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.FORMAT_DOCX,
            constants_.wordFormatText(),
            constants_.wordFormatDesc())
         );
      }
      else if (selectedTemplate.equals(TEMPLATE_PRESENTATION))
      {
         quartoPresentationsHelpLink_.setVisible(true);
         
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.FORMAT_REVEALJS,
            constants_.jsFormatText(),
            constants_.jsFormatDesc())
         );
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.FORMAT_BEAMER,
            constants_.beamerFormatText(),
            constants_.beamerFormatDesc())
         );
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.FORMAT_PPTX,
            constants_.powerPointFormatText(),
            constants_.powerPointFormatDesc()));
      }
      else if (selectedTemplate.equals(TEMPLATE_INTERACTIVE))
      {
         quartoInteractiveHelpLink_.setVisible(true);
         
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.INTERACTIVE_SHINY,
            "Shiny",
            constants_.shinyFormatDesc())
         );
         templateFormatPanel_.add(createFormatOption(
            QuartoCommandConstants.INTERACTIVE_OJS,
            "Observable JS",
            constants_.observableJSFormatDesc())
         );
      }
      // select the first visible format by default
      for (int i = 0; i < formatOptions_.size(); i++)
      {
         if (formatOptions_.get(i).getParent().isVisible())
         {
            formatOptions_.get(i).setValue(true);
            break;
         }
      }
      
      manageControls();
   }

   private Widget createFormatOption(String name, String text, String description)
   {
      HTMLPanel formatWrapper = new HTMLPanel("");
      formatWrapper.setStyleName(resources.styles().outputFormat());
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      sb.appendHtmlConstant("<span class=\"" + resources.styles().outputFormatName() + "\">");
      sb.appendEscaped(text);
      sb.appendHtmlConstant("</span>");
      RadioButton button = new RadioButton("DefaultOutputFormat", sb.toSafeHtml().asString(), true);
      button.addStyleName(resources.styles().outputFormatChoice());
      formatNames_.add(name);
      formatOptions_.add(button);
      formatWrapper.add(button);
      Label label = new Label(description);
      label.setStyleName(resources.styles().outputFormatDetails());
      formatWrapper.add(label);
      return formatWrapper;
   }
   
   private void manageControls()
   {
      RowFormatter rowFmt = grid_.getRowFormatter();
      // kernel only shows for jupyter
      rowFmt.setVisible(ROW_ENGINE, !getSelectedTemplate().equals(TEMPLATE_INTERACTIVE));
      rowFmt.setVisible(ROW_KERNEL, !getSelectedTemplate().equals(TEMPLATE_INTERACTIVE) &&
                                    engineSelect_.getSelectedValue().equals(QuartoCommandConstants.ENGINE_JUPYTER));
      rowFmt.setVisible(ROW_EDITOR, !getSelectedTemplate().equals(TEMPLATE_INTERACTIVE));
   }
   
   private Label createLabel(String caption)
   {
      Label label = new Label(caption);
      label.addStyleName(RES.styles().label());
      return label;
   }
   
   private ListBox createListBox(String[] items, String[] values)
   {
      ListBox listBox = createListBox();
      for (int i=0; i<items.length; i++)
         listBox.addItem(items[i], values[i]);
      return listBox;
   }
   
   private ListBox createListBox()
   {
      ListBox listBox = new ListBox();
      listBox.setMultipleSelect(false);
      listBox.addStyleName(RES.styles().listBox());
      listBox.addChangeHandler((event) -> {
         manageControls();
      });
      return listBox;
   }
   
   private HelpLink addHelpLink(String caption, String link)
   {
      HelpLink helpLink = new HelpLink(caption, link, false, false);
      helpLink.getElement().getStyle().setMarginTop(12, Unit.PX);
      newTemplatePanel_.add(helpLink);
      return helpLink;
   }
 
   
   private void setListBoxValue(ListBox listBox, String value)
   {
      for (int i=0; i<listBox.getItemCount(); i++)
      {
         if (listBox.getValue(i).equals(value)) 
         {
            listBox.setSelectedIndex(i);
            return;
         }
      }
   }
   
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }
   
   
   private class NewQuartoDocumentClientState extends JSObjectStateValue
   {
      public NewQuartoDocumentClientState()
      {
         super("quarto",
               "new-document",
               ClientState.PERSISTENT,
               session_.getSessionInfo().getClientState(),
               false);
      }

      @Override
      protected void onInit(JsObject value)
      {
         lastResult_ = (value == null) ?
               Result.createDefault() :
                  Result.create(
                     value.getString("title"),
                     value.getString("author"),
                     value.getString("format"),
                     value.getString("engine"),
                     value.getString("kernel"),
                     value.getString("language"),
                     value.getString("editor"),
                     false
                  );
      }
 
      @Override
      protected JsObject getValue()
      {
         return lastResult_.cast();
      }
   }

   private final void loadAndPersistClientState()
   {
      if (clientStateValue_ == null)
         clientStateValue_ = new NewQuartoDocumentClientState();
   }
   private static NewQuartoDocumentClientState clientStateValue_;
   private static Result lastResult_ = Result.createDefault();

   private final int ROW_ENGINE = 0;
   private final int ROW_KERNEL = 1;
   private final int ROW_EDITOR = 2;
   
   
   @UiField
   TextBox txtAuthor_;
   @UiField
   TextBox txtTitle_;
   @UiField
   WidgetListBox<TemplateMenuItem> listTemplates_;
   @UiField
   NewDocumentResources resources;
   @UiField
   HTMLPanel templateFormatPanel_;
   @UiField
   HTMLPanel newTemplatePanel_;
   @UiField
   LayoutGrid grid_;
   
   private final Widget mainWidget_;
   private List<String> formatNames_;
   private List<RadioButton> formatOptions_;
   
   private final ListBox engineSelect_;
   private final ListBox kernelSelect_;
   private QuartoVisualEditorCheckBox editorCheckBox_;
   
   private HelpLink quartoHelpLink_;
   private HelpLink quartoPresentationsHelpLink_;
   private HelpLink quartoInteractiveHelpLink_;
   
   private final QuartoCapabilities caps_;
   
   private Session session_;
   private GlobalDisplay globalDisplay_;
   
 // Styles ----
   
   public interface Styles extends CssResource
   {
      String mainPanel();
      String grid();
      String spacedRow();
      String label();
      String textBox();
      String listBox();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewQuartoDocumentDialog.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

   private static final QuartoConstants constants_ = GWT.create(QuartoConstants.class);
   private static final String TEMPLATE_DOCUMENT = constants_.documentLabel();
   private static final String TEMPLATE_PRESENTATION = constants_.presentationLabel();
   private static final String TEMPLATE_INTERACTIVE = constants_.interactiveLabel();
}
