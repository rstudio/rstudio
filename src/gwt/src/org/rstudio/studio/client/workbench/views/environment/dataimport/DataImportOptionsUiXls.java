/*
 * DataImportOptionsUiXls.java
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

import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportPreviewResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class DataImportOptionsUiXls extends DataImportOptionsUi
{

   private static DataImportOptionsUiXlsUiBinder uiBinder = GWT
         .create(DataImportOptionsUiXlsUiBinder.class);

   interface DataImportOptionsUiXlsUiBinder
         extends UiBinder<Widget, DataImportOptionsUiXls>
   {
   }

   public DataImportOptionsUiXls()
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      initDefaults();
      initEvents();
   }
   
   @Override
   public DataImportOptionsXls getOptions()
   {
      return DataImportOptionsXls.create(
         nameTextBox_.getValue(),
         !sheetListBox_.getSelectedValue().isEmpty() ? sheetListBox_.getSelectedValue() : null,
         Integer.parseInt(skipTextBox_.getValue()),
         columnNamesCheckBox_.getValue().booleanValue(),
         !naListBox_.getSelectedValue().isEmpty() ? naListBox_.getSelectedValue() : null,
         openDataViewerCheckBox_.getValue().booleanValue()
      );
   }
   
   @Override
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      nameTextBox_.setText(response.getDataName());
   }
   
   @Override
   public void setPreviewResponse(DataImportPreviewResponse response)
   {
      String[] sheets = getSheetsFromResponse(response);
      
      if (sheetListBox_.getItemCount() <= 1)
      {
         sheetListBox_.clear();
         sheetListBox_.addItem("Default", "");
         
         for (String sheet : sheets){
            sheetListBox_.addItem(sheet, sheet);
         }
      }
   }
   
   @Override
   public void clearOptions()
   {
      nameTextBox_.setText("");
      sheetListBox_.clear();
      sheetListBox_.addItem("Default", "");
   }
   
   void initDefaults()
   {
      skipTextBox_.setText("0");
      
      columnNamesCheckBox_.setValue(true);
      openDataViewerCheckBox_.setValue(true);
      
      sheetListBox_.addItem("Default", "");
      
      naListBox_.addItem("Default", "");
      naListBox_.addItem("NA", "NA");
      naListBox_.addItem("null", "null");
      naListBox_.addItem("0", "0");
      naListBox_.addItem("empty", "empty");
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
      sheetListBox_.addChangeHandler(changeHandler);
      columnNamesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      openDataViewerCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      naListBox_.addChangeHandler(changeHandler);
      skipTextBox_.addValueChangeHandler(valueChangeHandler);
   }
   
   @UiField
   ListBox sheetListBox_;
   
   @UiField
   TextBox skipTextBox_;
   
   @UiField
   CheckBox columnNamesCheckBox_;
   
   @UiField
   ListBox naListBox_;

   @UiField
   CheckBox openDataViewerCheckBox_;
   
   private static native final String[] getSheetsFromResponse(DataImportPreviewResponse response) /*-{
      return response && response.options && response.options.sheets ? response.options.sheets : [];
   }-*/;
}
