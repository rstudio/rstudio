/*
 * DataImportOptionsCsv.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class DataImportOptionsUiCsv extends DataImportOptionsUi
{

   private static DataImportOptionsCsvUiBinder uiBinder = GWT
         .create(DataImportOptionsCsvUiBinder.class);

   interface DataImportOptionsCsvUiBinder extends UiBinder<HTMLPanel, DataImportOptionsUiCsv> {}

   HTMLPanel mainPanel_;
   
   public DataImportOptionsUiCsv()
   {
      super();
      mainPanel_ = uiBinder.createAndBindUi(this);
      
      initWidget(mainPanel_);
      
      initDefaults();
   }
   
   @Override
   public DataImportOptionsCsv getOptions()
   {
      return new DataImportOptionsCsv(nameTextBox_.getValue(),
            delimiterListBox_.getSelectedValue().charAt(0),
            quotesListBox_.getSelectedValue(),
            escapeBackslashCheckBox_.getValue(),
            escapeDoubleCheckBox_.getValue(),
            columnNamesCheckBox_.getValue(),
            trimSpacesCheckBox_.getValue());
   }
   
   void initDefaults()
   {
      columnNamesCheckBox_.setValue(true);
      escapeDoubleCheckBox_.setValue(true);
      
      delimiterListBox_.addItem("Comma", ",");
      delimiterListBox_.addItem("Semicolon", ";");
      delimiterListBox_.addItem("Tab", "\t");
      delimiterListBox_.addItem("Whitespace", "");
      
      quotesListBox_.addItem("Single quote (')", "'");
      quotesListBox_.addItem("Double quote (\")", "\"");
      quotesListBox_.addItem("None", "");
   }
   
   @UiField
   TextBox nameTextBox_;
   
   @UiField
   ListBox delimiterListBox_;
   
   @UiField
   ListBox quotesListBox_;
   
   @UiField
   CheckBox escapeBackslashCheckBox_;
   
   @UiField
   CheckBox escapeDoubleCheckBox_;
   
   @UiField
   CheckBox columnNamesCheckBox_;
   
   @UiField
   CheckBox trimSpacesCheckBox_;
}
