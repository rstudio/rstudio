/*
 * DataImportColumnTypesMenu.java
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

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class DataImportColumnTypesMenu extends PopupPanel
{

   private static DataImportColumnTypesMenuUiBinder uiBinder = GWT
         .create(DataImportColumnTypesMenuUiBinder.class);

   private OperationWithInput<String> onTypeChange_;
   private OperationWithInput<String> onSelectionChange_;
   
   class MenuItem
   {
      private String name_;
      private Widget widget_;
      
      public MenuItem(String name, Widget widget)
      {
         name_ = name;
         widget_ = widget;
      }
      
      String getName()
      {
         return name_;
      }
      
      Widget getWidget()
      {
         return widget_;
      }
   }
   
   private ArrayList<MenuItem> menuItems_;
   private HashMap<String, MenuItem> menuItemsMap_;
   
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
      
      menuItemsMap_ = new HashMap<String, MenuItem>();
      
      menuItems_ = new ArrayList<MenuItem>();
      menuItems_.add(new MenuItem("include", (Widget)include_));
      menuItems_.add(new MenuItem("skip", (Widget)skip_));
      menuItems_.add(new MenuItem("only", (Widget)only_));
      menuItems_.add(new MenuItem("guess", (Widget)guess_));
      menuItems_.add(new MenuItem("character", (Widget)character_));
      menuItems_.add(new MenuItem("double", (Widget)double_));
      menuItems_.add(new MenuItem("integer", (Widget)integer_));
      menuItems_.add(new MenuItem("numeric", (Widget)numeric_));
      menuItems_.add(new MenuItem("logical", (Widget)logical_));
      menuItems_.add(new MenuItem("date", (Widget)date_));
      menuItems_.add(new MenuItem("time", (Widget)time_));
      menuItems_.add(new MenuItem("dateTime", (Widget)dateTime_));
      menuItems_.add(new MenuItem("factor", (Widget)factor_));
      
      for (int idx = 0; idx < menuItems_.size(); idx++)
      {
         menuItemsMap_.put(menuItems_.get(idx).getName(), menuItems_.get(idx));
      }
      
      errorPanel_.setVisible(false);
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
      for (final MenuItem elem : menuItems_)
      {
         elem.getWidget().setStyleName(style.entry());
      }
      
      include_.setStyleName(style.entrySelected());
      
      typesPanel_.setVisible(true);
      includePanel_.setVisible(true);
      errorPanel_.setVisible(false);
   }
   
   public void setVisibleColumns(String[] columnNames)
   {
      for (final MenuItem elem : menuItems_)
      {
         elem.getWidget().setVisible(false);
      }
      
      for (final String columnName : columnNames)
      {
         menuItemsMap_.get(columnName).getWidget().setVisible(true);
      }
   }
   
   public void setSelected(String entry)
   {
      
      if (entry == "skip")
      {
         include_.setStyleName(style.entry());
         only_.setStyleName(style.entry());
         skip_.setStyleName(style.entrySelected());
      }
      else if (entry == "only")
      {
         include_.setStyleName(style.entry());
         only_.setStyleName(style.entrySelected());
         skip_.setStyleName(style.entry());
      }
      else
      {
         for (final MenuItem elem : menuItems_)
         {
            if (entry == elem.getName())
            {
               elem.getWidget().setStyleName(style.entrySelected());
            }
         }
      }
   }
   
   public void setError(String error)
   {
      error_.setText(error);
      errorPanel_.setVisible(true);
      typesPanel_.setVisible(false);
      includePanel_.setVisible(false);
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
   
   @UiField
   HTMLPanel typesPanel_;
   
   @UiField
   HTMLPanel includePanel_;
   
   @UiField
   HTMLPanel errorPanel_;
   
   @UiField
   Label error_;
}
