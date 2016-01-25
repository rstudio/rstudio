/*
 * DataImportColumnTypesMenu.java
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

import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class DataImportColumnTypesMenu extends PopupPanel
{

   private static DataImportColumnTypesMenuUiBinder uiBinder = GWT
         .create(DataImportColumnTypesMenuUiBinder.class);

   private OperationWithInput<String> onTypeChange_;
   private OperationWithInput<String> onSelectionChange_;
   
   interface DataImportColumnTypesMenuUiBinder
         extends UiBinder<Widget, DataImportColumnTypesMenu>
   {
   }
   
   interface DataImportColumnTypesMenuCss extends CssResource {
      String entry();
      String entrySelected();
   }
   
   @UiField DataImportColumnTypesMenuCss style;

   public DataImportColumnTypesMenu()
   {
      super(true);
      setWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setOnChange(
      OperationWithInput<String> onTypeChange,
      OperationWithInput<String> onSelectionChange)
   {
      onTypeChange_ = onTypeChange;
      onSelectionChange_ = onSelectionChange;
   }
   
   public void resetSelected()
   {
      include_.setStyleName(style.entrySelected());
      skip_.setStyleName(style.entry());
      only_.setStyleName(style.entry());
      guess_.setStyleName(style.entry());
      character_.setStyleName(style.entry());
      double_.setStyleName(style.entry());
      integer_.setStyleName(style.entry());
      numeric_.setStyleName(style.entry());
      logical_.setStyleName(style.entry());
      date_.setStyleName(style.entry());
      time_.setStyleName(style.entry());
      dateTime_.setStyleName(style.entry());
      factor_.setStyleName(style.entry());
   }
   
   public void setSelected(String entry)
   {
      switch (entry)
      {
      case "skip":
         include_.setStyleName(style.entry());
         only_.setStyleName(style.entry());
         skip_.setStyleName(style.entrySelected());
         break;
      case "only":
         include_.setStyleName(style.entry());
         only_.setStyleName(style.entrySelected());
         skip_.setStyleName(style.entry());
         break;
      case "guess":
         guess_.setStyleName(style.entrySelected());
         break;
      case "character":
         character_.setStyleName(style.entrySelected());
         break;
      case "double":
         double_.setStyleName(style.entrySelected());
         break;
      case "integer":
         integer_.setStyleName(style.entrySelected());
         break;
      case "numeric":
         numeric_.setStyleName(style.entrySelected());
         break;
      case "logical":
         logical_.setStyleName(style.entrySelected());
         break;
      case "date":
         date_.setStyleName(style.entrySelected());
         break;
      case "time":
         time_.setStyleName(style.entrySelected());
         break;
      case "datetime":
         dateTime_.setStyleName(style.entrySelected());
         break;
      case "factor":
         factor_.setStyleName(style.entrySelected());
         break;
      }
   }
   
   @UiField
   Label include_;
   
   @UiHandler("include_")
   void onIncludeClick(ClickEvent e)
   {
      onSelectionChange_.execute("include");
   }
   
   @UiField
   Label skip_;
   
   @UiHandler("skip_")
   void onSkipClick(ClickEvent e)
   {
      onSelectionChange_.execute("skip");
   }
   
   @UiField
   Label only_;
   
   @UiHandler("only_")
   void onOnlyClick(ClickEvent e)
   {
      onSelectionChange_.execute("only");
   }
   
   @UiField
   Label  guess_;
   
   @UiHandler("guess_")
   void onGuessClick(ClickEvent e)
   {
      onTypeChange_.execute("guess");
   }
   
   @UiField
   Label  character_;
   
   @UiHandler("character_")
   void onCharacterClick(ClickEvent e)
   {
      onTypeChange_.execute("character");
   }
   
   @UiField
   Label double_;
   
   @UiHandler("double_")
   void onDoubleClick(ClickEvent e)
   {
      onTypeChange_.execute("double");
   }
   
   @UiField
   Label integer_;
   
   @UiHandler("integer_")
   void onIntegerClick(ClickEvent e)
   {
      onTypeChange_.execute("integer");
   }
   
   @UiField
   Label numeric_;
   
   @UiHandler("numeric_")
   void onNumericClick(ClickEvent e)
   {
      onTypeChange_.execute("numeric");
   }
   
   @UiField
   Label logical_;
   
   @UiHandler("logical_")
   void onLogicalClick(ClickEvent e)
   {
      onTypeChange_.execute("logical");
   }
   
   @UiField
   Label date_;
   
   @UiHandler("date_")
   void onDateClick(ClickEvent e)
   {
      onTypeChange_.execute("date");
   }
   
   @UiField
   Label time_;
   
   @UiHandler("time_")
   void onTimeClick(ClickEvent e)
   {
      onTypeChange_.execute("time");
   }
   
   @UiField
   Label dateTime_;
   
   @UiHandler("dateTime_")
   void onDateTimeClick(ClickEvent e)
   {
      onTypeChange_.execute("dateTime");
   }
   
   @UiField
   Label factor_;
   
   @UiHandler("factor_")
   void onFactorClick(ClickEvent e)
   {
      onTypeChange_.execute("factor");
   }
}
