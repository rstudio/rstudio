/*
 * ImportFileSettingsDialog.java
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
package org.rstudio.studio.client.workbench.views.workspace.dataimport;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.workspace.model.DataPreviewResult;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;

public class ImportFileSettingsDialog extends ModalDialog<ImportFileSettings>
{
   interface Resources extends ClientBundle
   {
      @Source("ImportFileSettingsDialog.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String varname();
      String input();
      String output();
      String inputLabel();
      String outputLabel();
      String header();
      String leftPanel();
      String list();
   }

   interface MyBinder extends UiBinder<Widget, ImportFileSettingsDialog> {}

   public ImportFileSettingsDialog(
         WorkspaceServerOperations server,
         FileSystemItem dataFile,
         String varname,
         String caption,
         OperationWithInput<ImportFileSettings> operation,
         GlobalDisplay globalDisplay)
   {
      super(caption, operation);
      server_ = server;
      dataFile_ = dataFile;
      globalDisplay_ = globalDisplay;

      Resources res = GWT.create(Resources.class);
      styles_ = res.styles();

      MyBinder binder = GWT.create(MyBinder.class);
      widget_ = binder.createAndBindUi(this);

      if (varname != null)
         varname_.setText(varname);
      else
         varname_.setText(dataFile.getStem()
                                .replace(" ", ".")
                                .replace("-", "."));

      separator_.addItem("Whitespace", "");
      separator_.addItem("Comma", ",");
      separator_.addItem("Semicolon", ";");
      separator_.addItem("Tab", "\t");

      decimal_.addItem("Period", ".");
      decimal_.addItem("Comma", ",");

      quote_.addItem("Double quote (\")", "\"");
      quote_.addItem("Single quote (')", "'");
      quote_.addItem("None", "");

      hookChangeEvents();

      ((TextAreaElement) input_.getElement().cast()).setReadOnly(true);
      ((TextAreaElement) outputPanel_.getElement().cast()).setReadOnly(true);

      progress_ = addProgressIndicator();

      setOkButtonCaption("Import");
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();

      separator_.setSelectedIndex(-1);
      quote_.setSelectedIndex(-1);

      loadData();
   }

   private void hookChangeEvents()
   {
      ValueChangeHandler<Boolean> valueChangeHandler = new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> booleanValueChangeEvent)
         {
            updateOutput();
         }
      };
      headingYes_.addValueChangeHandler(valueChangeHandler);
      headingNo_.addValueChangeHandler(valueChangeHandler);

      ChangeHandler changeHandler = new ChangeHandler()
      {
         public void onChange(ChangeEvent event)
         {
            updateOutput();
         }
      };
      separator_.addChangeHandler(changeHandler);
      decimal_.addChangeHandler(changeHandler);
      quote_.addChangeHandler(changeHandler);
   }

   private void updateOutput()
   {
      if (separator_.getSelectedIndex() < 0
          || quote_.getSelectedIndex() < 0
          || decimal_.getSelectedIndex() < 0)
      {
         return;
      }

      updateRequest_.invalidate();
      final Token invalidationToken = updateRequest_.getInvalidationToken();

      progress_.onProgress("Updating preview");
      server_.getOutputPreview(
            dataFile_.getPath(),
            headingYes_.getValue().booleanValue(),
            separator_.getValue(separator_.getSelectedIndex()),
            decimal_.getValue(decimal_.getSelectedIndex()),
            quote_.getValue(quote_.getSelectedIndex()),
            new ServerRequestCallback<DataPreviewResult>()
            {
               @Override
               public void onResponseReceived(DataPreviewResult response)
               {
                  if (invalidationToken.isInvalid())
                     return;

                  progress_.onProgress(null);
                  populateOutput(response);
               }

               @Override
               public void onError(ServerError error)
               {
                  if (invalidationToken.isInvalid())
                     return;

                  progress_.onProgress(null);
                  globalDisplay_.showErrorMessage(
                        "Error",
                        error.getUserMessage());
               }
            });
   }

   private void loadData()
   {
      final Token invalidationToken = updateRequest_.getInvalidationToken();

      progress_.onProgress("Detecting data format");
      server_.getDataPreview(
            dataFile_.getPath(),
            new ServerRequestCallback<DataPreviewResult>()
            {
               @Override
               public void onResponseReceived(DataPreviewResult response)
               {
                  input_.setHTML(toInputHtml(response));

                  if (invalidationToken.isInvalid())
                     return;
                  
                  progress_.onProgress(null);
                  populateOutput(response);
                  if (response.hasHeader())
                     headingYes_.setValue(true);
                  else
                     headingNo_.setValue(true);

                  selectByValue(separator_, response.getSeparator());
                  selectByValue(decimal_, response.getDecimal());
                  selectByValue(quote_, response.getQuote());
               }

               @Override
               public void onError(ServerError error)
               {
                  if (invalidationToken.isInvalid())
                     return;

                  progress_.onProgress(null);
                  globalDisplay_.showErrorMessage(
                        "Error",
                        error.getUserMessage());
               }
            });
   }

   private void selectByValue(ListBox listBox, String value)
   {
      for (int i = 0; i < listBox.getItemCount(); i++)
      {
         if (equal(listBox.getValue(i), value))
         {
            listBox.setSelectedIndex(i);
            return;
         }
      }
      listBox.setSelectedIndex(-1);
   }

   private boolean equal(String v1, String v2)
   {
      if (v1 == null ^ v2 == null)
         return false;
      if (v1 == null)
         return true;
      return v1.equals(v2);
   }

   private void populateOutput(DataPreviewResult result)
   {
      JsArray<JsObject> output = result.getOutput();
      JsArrayString names = result.getOutputNames();

      int rows = output.length();
      int cols = names.length();
      Grid grid = new Grid(rows + 1, cols);
      grid.setCellPadding(0);
      grid.setCellSpacing(0);
      grid.getRowFormatter().addStyleName(0, styles_.header());
      for (int col = 0; col < cols; col++)
         grid.setText(0, col, names.get(col));

      for (int row = 0; row < rows; row++)
      {
         for (int col = 0; col < cols; col++)
         {
            String val = output.get(row).getString(names.get(col), true);
            if (val == null)
               val = "NA";
            grid.setText(row + 1, col, val);
         }
      }

      outputPanel_.setWidget(grid);
   }

   private String toInputHtml(DataPreviewResult response)
   {
      String input = response.getInputLines();
      return input.replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("^ ", "&nbsp;")
            .replaceAll(" $", "&nbsp;")
            .replaceAll("\\t", "<span style=\"background-color: #EEE; color: #888; padding: 0 10px 0 10px\">&#8677;</span>");
   }

   @Override
   protected ImportFileSettings collectInput()
   {
      return new ImportFileSettings(
            dataFile_,
            varname_.getText().trim(),
            headingYes_.getValue(),
            separator_.getValue(separator_.getSelectedIndex()),
            decimal_.getValue(decimal_.getSelectedIndex()),
            quote_.getValue(quote_.getSelectedIndex()));
   }

   @Override
   protected boolean validate(ImportFileSettings input)
   {
      if (varname_.getText().trim().length() == 0)
      {
         varname_.setFocus(true);
         globalDisplay_.showErrorMessage("Variable Name Is Required",
                                         "Please provide a variable name.");
         return false;
      }

      return (headingYes_.getValue() || headingNo_.getValue())
            && separator_.getSelectedIndex() >= 0
            && quote_.getSelectedIndex() >= 0;
   }

   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }

   public static void ensureStylesInjected()
   {
      Resources res = GWT.create(Resources.class);
      res.styles().ensureInjected();
   }

   @UiField
   ListBox separator_;
   @UiField
   ListBox decimal_;
   @UiField
   ListBox quote_;
   @UiField
   HTML input_;
   @UiField
   SimplePanel outputPanel_;
   @UiField
   RadioButton headingYes_;
   @UiField
   RadioButton headingNo_;
   @UiField
   TextBox varname_;

   private final Widget widget_;
   private final WorkspaceServerOperations server_;
   private final FileSystemItem dataFile_;
   private final GlobalDisplay globalDisplay_;
   private ProgressIndicator progress_;
   private final Invalidation updateRequest_ = new Invalidation();
   private final Styles styles_;
}
