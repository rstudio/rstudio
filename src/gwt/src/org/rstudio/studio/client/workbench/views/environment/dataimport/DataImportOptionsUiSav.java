/*
 * DataImportOptionsUiSav.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class DataImportOptionsUiSav extends DataImportOptionsUi
{

   private static DataImportOptionsUiSavUiBinder uiBinder = GWT
         .create(DataImportOptionsUiSavUiBinder.class);

   interface DataImportOptionsUiSavUiBinder
         extends UiBinder<Widget, DataImportOptionsUiSav>
   {
   }

   public DataImportOptionsUiSav(DataImportModes mode)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      initDefaults(mode);
      initEvents();
   }
   
   void initDefaults(DataImportModes mode)
   {
      formatListBox_.addItem("SAV", "sav");
      formatListBox_.addItem("DTA", "dta");
      formatListBox_.addItem("POR", "por");
      formatListBox_.addItem("SAS", "sas");
      formatListBox_.addItem("Stata", "stata");
      
      switch(mode)
      {
      case SAS:
         formatListBox_.setSelectedIndex(3);
         break;
      case Stata:
         formatListBox_.setSelectedIndex(4);
         break;
      default:
         formatListBox_.setSelectedIndex(0);
         break;
      }

      openDataViewerCheckBox_.setValue(true);
      
      updateEnabled();
   }
   
   @Override
   public DataImportOptionsSav getOptions()
   {
      return DataImportOptionsSav.create(
         nameTextBox_.getValue(),
         !fileChooser_.getText().isEmpty() ? fileChooser_.getText() : null,
         formatListBox_.getSelectedValue(),
         openDataViewerCheckBox_.getValue().booleanValue()
      );
   }
   
   @Override
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      nameTextBox_.setText(response.getDataName());
      updateEnabled();
   }
   
   @Override
   public void clearOptions()
   {
      nameTextBox_.setText("");
      updateEnabled();
   }
   
   @Override
   public void setImportLocation(String importLocation)
   {
      nameTextBox_.setText("");
      
      String[] components = importLocation.split("\\.");
      if (components.length > 0)
      {
         String extension = components[components.length - 1].toLowerCase();
         for (int idx = 0; idx < formatListBox_.getItemCount(); idx++)
         {
            if (formatListBox_.getValue(idx) == extension)
            {
               formatListBox_.setSelectedIndex(idx);
            }
         }
      }
   }
   
   @Override
   public HelpLink getHelpLink()
   {
      return new HelpLink(
         "Reading data using haven",
         "import_haven",
         false,
         true);
   }
   
   @UiFactory
   DataImportFileChooser makeLocationChooser()
   {
      DataImportFileChooser dataImportFileChooser = new DataImportFileChooser(
         new Operation()
         {
            @Override
            public void execute()
            {
               updateEnabled();
               triggerChange();
            }
         },
         false);
      
      return dataImportFileChooser;
   }
   
   void initEvents()
   {
      ValueChangeHandler<String> valueChangeHandler = new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> arg0)
         {
            updateEnabled();
            triggerChange();
         }
      };
      
      ChangeHandler changeHandler = new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent arg0)
         {
            updateEnabled();
            triggerChange();
         }
      };

      ValueChangeHandler<Boolean> booleanValueChangeHandler = new ValueChangeHandler<Boolean>()
      {
         
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> arg0)
         {
            updateEnabled();
            triggerChange();
         }
      };
      
      nameTextBox_.addValueChangeHandler(valueChangeHandler);
      formatListBox_.addChangeHandler(changeHandler);
      openDataViewerCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
   }
   
   void updateEnabled()
   {
      if (formatListBox_.getSelectedValue() == "sas")
      {
         fileChooser_.setEnabled(true);    
      }
      else
      {
         fileChooser_.setEnabled(false);
      }
   }
   
   @UiField
   ListBox formatListBox_;
   
   @UiField
   DataImportFileChooser fileChooser_;

   @UiField
   CheckBox openDataViewerCheckBox_;
}
