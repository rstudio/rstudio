/*
 * DataImportOptionsUiCsvLocale.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataImportOptionsUiCsvLocale extends ModalDialog<DataImportOptionsCsvLocale>
{
   interface Binder extends UiBinder<Widget, DataImportOptionsUiCsvLocale> {}
   
   public interface DataImportOptionsUiCsvLocaleStyle extends CssResource
   {
      String dialog();
   }

   @Inject
   private void initialize()
   {
   }
   
   public DataImportOptionsUiCsvLocale(
      OperationWithInput<DataImportOptionsCsvLocale> operation,
      DataImportOptionsCsvLocale locale)
   {
      super("Configure Locale", operation);
      widget_ = GWT.<Binder> create(Binder.class).createAndBindUi(this);
      initialLocale_ = locale;
   }
   
   private void assignLocale(DataImportOptionsCsvLocale locale)
   {
      if (locale == null) return;
      
      for (int idxEncoding = 0; idxEncoding < encoding_.getItemCount(); idxEncoding ++) {
         if (encoding_.getValue(idxEncoding) == locale.getEncoding()) {
            encoding_.setSelectedIndex(idxEncoding);
         }
      }
      
      dateName_.setText(locale.getDateName());
      dateFormat_.setText(locale.getDateFormat());
      timeFormat_.setText(locale.getTimeFormat());
      decimalMark_.setText(locale.getDecimalMark());
      groupingMark_.setText(locale.getGroupingMark());
      timeZone_.setText(locale.getTZ());
      asciify_.setValue(locale.getAsciify());
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();

      initializeDefaults();
      assignLocale(initialLocale_);

      setOkButtonCaption("Configure");

      HelpLink helpLink = new HelpLink(
         "Locales in readr",
         "readr_locales",
         false);
      addLeftWidget(helpLink);   
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }
   
   @Override
   protected DataImportOptionsCsvLocale collectInput()
   {
      return DataImportOptionsCsvLocale.createLocale(
        dateName_.getText(),
        dateFormat_.getText(),
        timeFormat_.getText(),
        decimalMark_.getText(),
        groupingMark_.getText(),
        timeZone_.getText(),
        encoding_.getSelectedValue(),
        asciify_.getValue()
      );
   }
   
   @Inject
   void initialize(FileTypeCommands fileTypeCommands)
   {
      initializeDefaults();
   }

   private void initializeDefaults()
   {
      encoding_.addItem("ASCII", "ASCII");
      encoding_.addItem("UTF-16", "UTF-16");
      encoding_.addItem("UTF-16BE", "UTF-16BE");
      encoding_.addItem("UTF-16LE", "UTF-16LE");
      encoding_.addItem("UTF-32", "UTF-32");
      encoding_.addItem("UTF-32BE", "UTF-32BE");
      encoding_.addItem("UTF-32LE", "UTF-32LE");
      encoding_.addItem("UTF-7", "UTF-7");
      encoding_.addItem("UTF-8", "UTF-8");
      encoding_.addItem("UTF-8-MAC", "UTF-8-MAC");
      encoding_.addItem("UTF8", "UTF8");
      encoding_.addItem("UTF8-MAC", "UTF8-MAC");
      
      encoding_.setSelectedIndex(8);
   }

   
   @UiField
   TextBox dateName_;

   @UiField
   ListBox encoding_;
   
   @UiField
   TextBox dateFormat_;
   
   @UiField
   TextBox timeFormat_;
   
   @UiField
   TextBox decimalMark_;
   
   @UiField
   TextBox groupingMark_;
   
   @UiField
   TextBox timeZone_;
   
   @UiField
   CheckBox asciify_;
   
   private Widget widget_;
   private DataImportOptionsCsvLocale initialLocale_;
}
