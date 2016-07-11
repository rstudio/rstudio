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

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

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
import com.google.inject.Inject;

public class DataImportOptionsUiCsv extends DataImportOptionsUi
{

   private static DataImportOptionsCsvUiBinder uiBinder = GWT
         .create(DataImportOptionsCsvUiBinder.class);

   private final String escapeBoth_ = "both";
   private final String escapeBackslash_ = "backslash";
   private final String escapeDouble_ = "double";
   
   interface DataImportOptionsCsvUiBinder extends UiBinder<HTMLPanel, DataImportOptionsUiCsv> {}

   HTMLPanel mainPanel_;
   
   SourceServerOperations sourceServer_;
   
   @Inject
   public DataImportOptionsUiCsv()
   {
      super();
      mainPanel_ = uiBinder.createAndBindUi(this);
      
      initWidget(mainPanel_);
      
      initEvents();
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(SourceServerOperations sourceServer)
   {
      sourceServer_ = sourceServer;
      
      initDefaults();
   }
   
   private boolean isBackslashValue(String value)
   {
      return value == escapeBoth_ || value == escapeBackslash_;
   }
   
   private boolean isDoubleValue(String value)
   {
      return value == escapeBoth_ || value == escapeDouble_;
   }
   
   @Override
   public DataImportOptionsCsv getOptions()
   {
      return DataImportOptionsCsv.create(nameTextBox_.getValue(),
            delimiterListBox_.getSelectedValue(),
            !quotesListBox_.getSelectedValue().isEmpty() ? quotesListBox_.getSelectedValue() : null,
            isBackslashValue(escapeListBox_.getSelectedValue()),
            isDoubleValue(escapeListBox_.getSelectedValue()),
            columnNamesCheckBox_.getValue().booleanValue(),
            trimSpacesCheckBox_.getValue().booleanValue(),
            !localeListBox_.getSelectedValue().isEmpty() ? localeListBox_.getSelectedValue() : null,
            !naListBox_.getSelectedValue().isEmpty() ? naListBox_.getSelectedValue() : null,
            !commentListBox_.getSelectedValue().isEmpty() ? commentListBox_.getSelectedValue() : null,
            Integer.parseInt(skipTextBox_.getText()),
            openDataViewerCheckBox_.getValue().booleanValue());
   }
   
   @Override
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      nameTextBox_.setText(response.getDataName());
   }
   
   @Override
   public void clearOptions()
   {
      nameTextBox_.setText("");
   }
   
   void initDefaults()
   {
      skipTextBox_.setText("0");
      
      columnNamesCheckBox_.setValue(true);
      trimSpacesCheckBox_.setValue(true);
      openDataViewerCheckBox_.setValue(true);
      
      escapeListBox_.addItem("None", "");
      escapeListBox_.addItem("Backslash", escapeBackslash_);
      escapeListBox_.addItem("Double", escapeDouble_);
      escapeListBox_.addItem("Both", escapeBoth_);
      
      delimiterListBox_.addItem("Comma", ",");
      delimiterListBox_.addItem("Semicolon", ";");
      delimiterListBox_.addItem("Tab", "\t");
      delimiterListBox_.addItem("Whitespace", " ");
      
      quotesListBox_.addItem("Default", "");
      quotesListBox_.addItem("Single (')", "'");
      quotesListBox_.addItem("Double (\")", "\\\"");
      quotesListBox_.addItem("None", "");
      
      naListBox_.addItem("Default", "");
      naListBox_.addItem("NA", "NA");
      naListBox_.addItem("null", "null");
      naListBox_.addItem("0", "0");
      naListBox_.addItem("empty", "empty");
      
      commentListBox_.addItem("Default", "");
      commentListBox_.addItem("#", "#");
      commentListBox_.addItem("%", "%");
      commentListBox_.addItem("//", "//");
      commentListBox_.addItem("'", "'");
      commentListBox_.addItem("!", "!");
      commentListBox_.addItem(";", ";");
      commentListBox_.addItem("--", "--");
      commentListBox_.addItem("*", "*");
      commentListBox_.addItem("||", "||");
      commentListBox_.addItem("\"", "\"");
      commentListBox_.addItem("\\", "\\");
      commentListBox_.addItem("*>", "*>");
      
      localeListBox_.addItem("Default", "");
      localeListBox_.addItem("ASCII", "ASCII");
      localeListBox_.addItem("UTF-16", "UTF-16");
      localeListBox_.addItem("UTF-16BE", "UTF-16BE");
      localeListBox_.addItem("UTF-16LE", "UTF-16LE");
      localeListBox_.addItem("UTF-32", "UTF-32");
      localeListBox_.addItem("UTF-32BE", "UTF-32BE");
      localeListBox_.addItem("UTF-32LE", "UTF-32LE");
      localeListBox_.addItem("UTF-7", "UTF-7");
      localeListBox_.addItem("UTF-8", "UTF-8");
      localeListBox_.addItem("UTF-8-MAC", "UTF-8-MAC");
      localeListBox_.addItem("UTF8", "UTF8");
      localeListBox_.addItem("UTF8-MAC", "UTF8-MAC");
     
      updateEnabled();
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
      delimiterListBox_.addChangeHandler(changeHandler);
      quotesListBox_.addChangeHandler(changeHandler);
      escapeListBox_.addChangeHandler(changeHandler);
      columnNamesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      trimSpacesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      openDataViewerCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      localeListBox_.addChangeHandler(changeHandler);
      naListBox_.addChangeHandler(changeHandler);
      commentListBox_.addChangeHandler(changeHandler);
      skipTextBox_.addValueChangeHandler(valueChangeHandler);
   }
   
   void updateEnabled()
   {
      if (delimiterListBox_.getSelectedValue() == ",")
      {
         trimSpacesCheckBox_.setEnabled(true);
         escapeListBox_.getElement().setAttribute("disabled", "disabled");
         quotesListBox_.getElement().setAttribute("disabled", "disabled");         
      }
      else
      {
         trimSpacesCheckBox_.setEnabled(false);
         escapeListBox_.getElement().removeAttribute("disabled");
         quotesListBox_.getElement().removeAttribute("disabled");
      }
   }
   
   @UiField
   TextBox skipTextBox_;
   
   @UiField
   ListBox delimiterListBox_;
   
   @UiField
   ListBox quotesListBox_;
   
   @UiField
   ListBox localeListBox_;
   
   @UiField
   ListBox naListBox_;
   
   @UiField
   ListBox commentListBox_;
   
   @UiField
   ListBox escapeListBox_;
   
   @UiField
   CheckBox columnNamesCheckBox_;
   
   @UiField
   CheckBox trimSpacesCheckBox_;

   @UiField
   CheckBox openDataViewerCheckBox_;
}
