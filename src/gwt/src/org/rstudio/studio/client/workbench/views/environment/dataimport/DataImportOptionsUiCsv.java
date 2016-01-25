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

import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
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

   private final String escapeBoth_ = "both";
   private final String escapeBackslash_ = "backslash";
   private final String escapeDouble_ = "double";
   
   interface DataImportOptionsCsvUiBinder extends UiBinder<HTMLPanel, DataImportOptionsUiCsv> {}

   HTMLPanel mainPanel_;
   
   public DataImportOptionsUiCsv()
   {
      super();
      mainPanel_ = uiBinder.createAndBindUi(this);
      
      initWidget(mainPanel_);
      
      initDefaults();
      initEvents();
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
            Integer.parseInt(skipTextBox_.getText()));
   }
   
   @Override
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      nameTextBox_.setText(response.getDataName());
   }
   
   @Override
   public void clearDataName()
   {
      nameTextBox_.setText("");
   }
   
   void initDefaults()
   {
      skipTextBox_.setText("0");
      
      columnNamesCheckBox_.setValue(true);
      trimSpacesCheckBox_.setValue(true);
      
      escapeListBox_.addItem("none", "");
      escapeListBox_.addItem("Backslash", escapeBackslash_);
      escapeListBox_.addItem("Double", escapeDouble_);
      escapeListBox_.addItem("Both", escapeBoth_);
      
      delimiterListBox_.addItem("Comma", ",");
      delimiterListBox_.addItem("Semicolon", ";");
      delimiterListBox_.addItem("Tab", "\t");
      delimiterListBox_.addItem("Whitespace", "");
      
      quotesListBox_.addItem("default", "");
      quotesListBox_.addItem("Single (')", "'");
      quotesListBox_.addItem("Double (\")", "\\\"");
      quotesListBox_.addItem("None", "");
      
      naListBox_.addItem("default", "");
      naListBox_.addItem("NA", "NA");
      naListBox_.addItem("null", "null");
      naListBox_.addItem("0", "0");
      naListBox_.addItem("empty", "empty");
      
      commentListBox_.addItem("default", "");
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
      
      String langs[] = {
         "af","agq","ak","am","ar","as","asa","az","bas","be","bem","bez",
         "bg","bm","bn","bo","br","brx","bs","ca","cgg","chr","cs","cy","da",
         "dav","de","dje","dsb","dua","dyo","dz","ebu","ee","el","en","eo",
         "es","et","eu","ewo","fa","ff","fi","fil","fo","fr","fur","fy","ga",
         "gd","gl","gsw","gu","guz","gv","ha","haw","he","hi","hr","hsb","hu",
         "hy","id","ig","ii","is","it","ja","jgo","jmc","ka","kab","kam","kde",
         "kea","khq","ki","kk","kkj","kl","kln","km","kn","ko","kok","ks","ksb",
         "ksf","ksh","kw","ky","lag","lb","lg","lkt","ln","lo","lt","lu","luo",
         "luy","lv","mas","mer","mfe","mg","mgh","mgo","mk","ml","mn","mr","ms",
         "mt","mua","my","naq","nb","nd","ne","nl","nmg","nn","nnh","nus","nyn",
         "om","or","os","pa","pl","ps","pt","qu","rm","rn","ro","rof","ru","rw",
         "rwk","sah","saq","sbp","se","seh","ses","sg","shi","si","sk","sl",
         "smn","sn","so","sq","sr","sv","sw","ta","te","teo","th","ti","to","tr",
         "twq","tzm","ug","uk","ur","uz","vai","vi","vun","wae","xog","yav","yi",
         "yo","zgh","zh","zu"
      };
      
      localeListBox_.addItem("default", "");
      for(String lang : langs) {
         localeListBox_.addItem(lang, lang);
      }
   }
   
   void triggerChange()
   {
      ValueChangeEvent.fire(this, getOptions());
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
      escapeListBox_.addChangeHandler(changeHandler);
      columnNamesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      trimSpacesCheckBox_.addValueChangeHandler(booleanValueChangeHandler);
      localeListBox_.addChangeHandler(changeHandler);
      naListBox_.addChangeHandler(changeHandler);
      commentListBox_.addChangeHandler(changeHandler);
      skipTextBox_.addValueChangeHandler(valueChangeHandler);
   }
   
   @UiField
   TextBox nameTextBox_;
   
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
   
   private final HandlerManager handlerManager_ = new HandlerManager(this);

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<DataImportOptions> handler)
   {
      return handlerManager_.addHandler(
            ValueChangeEvent.getType(),
            handler);
   }
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event);
   }
}
