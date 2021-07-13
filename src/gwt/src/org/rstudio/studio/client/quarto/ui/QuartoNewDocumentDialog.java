/*
 * QuartoNewDocumentDialog.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.FieldSetWrapperPanel;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.quarto.model.QuartoCapabilities;
import org.rstudio.studio.client.quarto.model.QuartoConstants;
import org.rstudio.studio.client.quarto.model.QuartoJupyterKernel;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLTable.RowFormatter;
import com.google.inject.Inject;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class QuartoNewDocumentDialog extends ModalDialog<QuartoNewDocumentDialog.Result>
{
   // extends JavaScriptObject for easy serialization (as client state)
   public static class Result extends JavaScriptObject
   {
      protected Result() {}

      public static final Result createDefault()
      {
         return create("", "", "html", "default", false, false, "knitr", "python3", "python");
      }

      public static final native Result create(String title,
                                               String author,
                                               String format,
                                               String theme,
                                               boolean tableOfContents,
                                               boolean numberSections,
                                               String engine,
                                               String kernel,
                                               String language)
      /*-{
         return {
            "title": title,
            "author": author,
            "format": format,
            "theme": theme,
            "tableOfContents": tableOfContents,
            "numberSections": numberSections,
            "engine": engine,
            "kernel": kernel,
            "language": language
         };
      }-*/;

      public final native String getTitle() /*-{ return this["title"]; }-*/;
      public final native String getAuthor() /*-{ return this["author"]; }-*/;
      public final native String getFormat() /*-{ return this["format"]; }-*/;
      public final native String getTheme() /*-{ return this["theme"]; }-*/;
      public final native boolean getTableOfContents() /*-{ return this["tableOfContents"]; }-*/;
      public final native boolean getNumberSections() /*-{ return this["numberSections"]; }-*/;
      public final native String getEngine() /*-{ return this["engine"]; }-*/;
      public final native String getKernel() /*-{ return this["kernel"]; }-*/;
      public final native String getLanguage() /*-{ return this["language"]; }-*/;
   }
   
   public QuartoNewDocumentDialog(QuartoCapabilities caps,
                                  OperationWithInput<Result> operation)
   {
      super("New Quarto Document", Roles.getDialogRole(), operation);
      RStudioGinjector.INSTANCE.injectMembers(this);

      loadAndPersistClientState();
      
      setOkButtonCaption("Create");
      
      caps_ = caps;
      
      mainPanel_ = new VerticalPanel();
      mainPanel_.addStyleName(RES.styles().mainPanel());
      
      titleTextBox_ = createTextBox();
      titleTextBox_.setText("Untitled");
      FormLabel titleLabel =  createFormLabel("Title:", titleTextBox_);
   
      authorTextBox_ = createTextBox();
      authorTextBox_.setText(lastResult_.getAuthor());
      FormLabel authorLabel = createFormLabel("Author:", authorTextBox_);
      DomUtils.setPlaceholder(authorTextBox_, "(Optional)");
      
      formatSelect_ = createListBox(caps.getFormats());
      setListBoxValue(formatSelect_, lastResult_.getFormat());
      FormLabel formatSelectLabel = createFormLabel("Format:", formatSelect_);
      
      themeSelect_ = createListBox(caps.getThemes());
      setListBoxValue(themeSelect_, lastResult_.getTheme());
      FormLabel themeSelectLabel = createFormLabel("Theme:", themeSelect_);
      
      Label optionsLabel = createLabel("Sections:");
      FieldSetWrapperPanel<VerticalPanel> optionsPanel = new FieldSetWrapperPanel<>(new VerticalPanel(), optionsLabel);
      optionsPanel.add(tableOfContentsCheckBox_ = new CheckBox("Table of contents"));
      tableOfContentsCheckBox_.setValue(lastResult_.getTableOfContents());
      optionsPanel.add(numberSectionsCheckBox_ = new CheckBox("Number sections"));
      numberSectionsCheckBox_.setValue(lastResult_.getNumberSections());
      
      Label engineLabel = createLabel("Engine:");
      engineSelect_ = createListBox(
         new String[] {"(None)", "Knitr", "Jupyter"},
         new String[] {
           QuartoConstants.ENGINE_MARKDOWN, 
           QuartoConstants.ENGINE_KNITR, 
           QuartoConstants.ENGINE_JUPYTER
         }
      );
      setListBoxValue(engineSelect_, lastResult_.getEngine());
            
      Label kernelLabel = createLabel("Kernel:");
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
      
      // Add them to parent
      grid_ = new LayoutGrid(7, 2);
      grid_.setCellPadding(2);
      grid_.addStyleName(RES.styles().grid());
      grid_.setWidget(ROW_TITLE, 0, titleLabel);
      grid_.setWidget(ROW_TITLE, 1, titleTextBox_);
      grid_.setWidget(ROW_AUTHOR, 0, authorLabel);
      grid_.setWidget(ROW_AUTHOR, 1, authorTextBox_);
      
      grid_.setWidget(ROW_FORMAT, 0, formatSelectLabel);
      grid_.setWidget(ROW_FORMAT, 1, formatSelect_);
      
      grid_.setWidget(ROW_THEME, 0, themeSelectLabel);
      grid_.setWidget(ROW_THEME, 1, themeSelect_);
      
      grid_.setWidget(ROW_OPTIONS, 0, optionsLabel);
      grid_.setWidget(ROW_OPTIONS, 1, optionsPanel);
      
      grid_.setWidget(ROW_ENGINE, 0, engineLabel);
      grid_.setWidget(ROW_ENGINE, 1, engineSelect_);
      
      grid_.setWidget(ROW_KERNEL, 0, kernelLabel);
      grid_.setWidget(ROW_KERNEL, 1, kernelSelect_);
      
     
      // tweak some row spacing
      RowFormatter rowFmt = grid_.getRowFormatter();
      rowFmt.addStyleName(ROW_AUTHOR, RES.styles().spacedRow());
      rowFmt.addStyleName(ROW_OPTIONS, RES.styles().spacedRow());
      
      mainPanel_.add(grid_);
      
      mainPanel_.add(new VerticalSpacer("12px"));
      
      manageControls();
      
      HelpLink quartoHelpLink = new HelpLink(
            "About Quarto",
            "https://quarto.org",
            false, false);
      quartoHelpLink.getElement().getStyle().setMarginTop(4, Unit.PX);
      addLeftWidget(quartoHelpLink);
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
      titleTextBox_.setFocus(true);
      titleTextBox_.selectAll();
   }
   
   @Override
   protected boolean validate(Result result)
   {
      String title = result.getTitle();
      if (StringUtil.isNullOrEmpty(title))
      {
         globalDisplay_.showErrorMessage(
            "Title Required", 
            "You must provide a title for the document", 
            titleTextBox_);
         return false;
      }
      
      return true;   
   }

   @Override
   protected Result collectInput()
   {
      String title = titleTextBox_.getText().trim();
      String author = authorTextBox_.getText().trim();
      String format = formatSelect_.getSelectedValue();
      String theme = themeSelect_.getSelectedValue();
      Boolean toc = tableOfContentsCheckBox_.getValue();
      Boolean number = numberSectionsCheckBox_.getValue();
      String engine = engineSelect_.getSelectedValue();
      String kernel = kernelSelect_.getSelectedValue();
      
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
     
      // result
      lastResult_ =  Result.create(title, author, format, theme, toc, number, engine, kernel, language);
      return Result.create(title, author, format, theme, toc, number, engine, kernel, language);
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainPanel_;
   }
   
   private void manageControls()
   {
      RowFormatter rowFmt = grid_.getRowFormatter();
      rowFmt.setVisible(ROW_THEME, formatSelect_.getSelectedValue().equals(QuartoConstants.FORMAT_HTML));
      rowFmt.setVisible(ROW_KERNEL, engineSelect_.getSelectedValue().equals(QuartoConstants.ENGINE_JUPYTER));
   }
   
   private FormLabel createFormLabel(String caption, Widget w)
   {
      FormLabel label = new FormLabel(caption, w);
      label.addStyleName(RES.styles().label());
      return label;
   }
   
   private Label createLabel(String caption)
   {
      Label label = new Label(caption);
      label.addStyleName(RES.styles().label());
      return label;
   }
   
   private TextBox createTextBox()
   {
      TextBox textBox = new TextBox();
      textBox.addStyleName(RES.styles().textBox());
      DomUtils.disableSpellcheck(textBox);
      return textBox;
   }
   
   private ListBox createListBox(JsArrayString choices)
   {
      ListBox listBox = createListBox();
      for (int i=0; i<choices.length(); i++)
         listBox.addItem(choices.get(i));
      return listBox;
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
   
   
   private class QuartoNewDocumentClientState extends JSObjectStateValue
   {
      public QuartoNewDocumentClientState()
      {
         super("quarto",
               "quarto-new-document",
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
                        value.getString("theme"),
                        value.getBoolean("tableOfContents"),
                        value.getBoolean("numberSections"),
                        value.getString("engine"),
                        value.getString("kernel"),
                        value.getString("language")
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
         clientStateValue_ = new QuartoNewDocumentClientState();
   }
   private static QuartoNewDocumentClientState clientStateValue_;
   private static Result lastResult_ = Result.createDefault();

   private final int ROW_TITLE = 0;
   private final int ROW_AUTHOR = 1;
   private final int ROW_FORMAT = 2;
   private final int ROW_THEME = 3;
   private final int ROW_OPTIONS = 4;
   private final int ROW_ENGINE = 5;
   private final int ROW_KERNEL = 6;
   
   private final TextBox titleTextBox_;
   private final TextBox authorTextBox_;
   private final ListBox formatSelect_;
   private final ListBox themeSelect_;
   private final CheckBox tableOfContentsCheckBox_;
   private final CheckBox numberSectionsCheckBox_;
   private final ListBox engineSelect_;
   private final ListBox kernelSelect_;
   
   private final LayoutGrid grid_;
   private final VerticalPanel mainPanel_;
   
   private Session session_;
   private GlobalDisplay globalDisplay_;
   
   private final QuartoCapabilities caps_;
   
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
      @Source("QuartoNewDocumentDialog.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
  
}
