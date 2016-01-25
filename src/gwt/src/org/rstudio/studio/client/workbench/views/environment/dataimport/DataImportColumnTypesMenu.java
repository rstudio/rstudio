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

   private OperationWithInput<String> onChange_;
   
   interface DataImportColumnTypesMenuUiBinder
         extends UiBinder<Widget, DataImportColumnTypesMenu>
   {
   }

   public DataImportColumnTypesMenu()
   {
      super(true);
      setWidget(uiBinder.createAndBindUi(this));
   }
   
   public void setOnChange(OperationWithInput<String> onChange)
   {
      onChange_ = onChange;
   }
   
   @UiField
   Label skip_;
   
   @UiHandler("skip_")
   void onSkipClick(ClickEvent e)
   {
      onChange_.execute("skip");
   }
   
   @UiField
   Label include_;
   
   @UiHandler("include_")
   void onIncludeClick(ClickEvent e)
   {
      onChange_.execute("include");
   }
   
   @UiField
   Label  character_;
   
   @UiHandler("character_")
   void onCharacterClick(ClickEvent e)
   {
      onChange_.execute("character");
   }
   
   @UiField
   Label double_;
   
   @UiHandler("double_")
   void onDoubleClick(ClickEvent e)
   {
      onChange_.execute("double");
   }
   
   @UiField
   Label integer_;
   
   @UiHandler("integer_")
   void onIntegerClick(ClickEvent e)
   {
      onChange_.execute("integer");
   }
   
   @UiField
   Label numeric_;
   
   @UiHandler("numeric_")
   void onNumericClick(ClickEvent e)
   {
      onChange_.execute("numeric");
   }
   
   @UiField
   Label logical_;
   
   @UiHandler("logical_")
   void onLogicalClick(ClickEvent e)
   {
      onChange_.execute("logical");
   }
   
   @UiField
   Label date_;
   
   @UiHandler("date_")
   void onDateClick(ClickEvent e)
   {
      onChange_.execute("date");
   }
   
   @UiField
   Label time_;
   
   @UiHandler("time_")
   void onTimeClick(ClickEvent e)
   {
      onChange_.execute("time");
   }
   
   @UiField
   Label dateTime_;
   
   @UiHandler("dateTime_")
   void onDateTimeClick(ClickEvent e)
   {
      onChange_.execute("dateTime");
   }
   
   @UiField
   Label factor_;
   
   @UiHandler("factor_")
   void onFactorClick(ClickEvent e)
   {
      onChange_.execute("factor");
   }
}
