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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
   DataImportScript dataImportScript_;
   
   public DataImportOptionsUiCsv(DataImportScript dataImportScript)
   {
      super();
      mainPanel_ = uiBinder.createAndBindUi(this);
      dataImportScript_ = dataImportScript;
      
      initWidget(mainPanel_);
      
      initDefaults();
      initEvents();
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
   
   @Override
   public void addChangeHandler(ChangeHandler changeHandler)
   {
      changeHandlers_.add(changeHandler);
   }
   
   @Override
   public String getCodePreview(DataImportOptions options)
   {
      return dataImportScript_.getImportScript(DataImportModes.Csv, options);
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
   
   void triggerChange()
   {
      for (ChangeHandler changeHandler : changeHandlers_) {
         changeHandler.onChange(null);
      }
   }
   
   void initEvents()
   {
      ValueChangeHandler<String> valueChangeHandler = new ValueChangeHandler<String>()
      {
         
         @Override
         public void onValueChange(ValueChangeEvent<String> arg0)
         {
            triggerChange();
         }
      };
      
      ChangeHandler changeHandler = new ChangeHandler()
      {
         
         @Override
         public void onChange(ChangeEvent arg0)
         {
            triggerChange();
         }
      };
      
      ValueChangeHandler<Boolean> booleanValueChangeHandler = new ValueChangeHandler<Boolean>()
      {
         
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> arg0)
         {
            triggerChange();
         }
      };
      
      nameTextBox_.addValueChangeHandler(valueChangeHandler);
      delimiterListBox_.addChangeHandler(changeHandler);
      quotesListBox_.addChangeHandler(changeHandler);
      escapeBackslashCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      escapeDoubleCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      columnNamesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      trimSpacesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
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
   
   List<ChangeHandler> changeHandlers_ = new ArrayList<ChangeHandler>();
}
