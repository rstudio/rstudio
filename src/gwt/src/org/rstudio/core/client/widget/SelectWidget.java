/*
 * SelectWidget.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.EnumValue;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class SelectWidget extends Composite
                          implements CanSetControlId
{
   public static String ExternalLabel = null;

   public SelectWidget()
   {
      this(ExternalLabel);
   }

   public SelectWidget(String label,
                       boolean useStyles,
                       PrefValue<String> prefValue)
   {
      this(
            label,
            prefValue,
            false,
            true,
            false);
      
      if (!useStyles)
      {
         removeStyleName(ThemeResources.INSTANCE.themeStyles().selectWidget());
      }
   }
   
   public SelectWidget(EnumValue enumValue,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft)
   {
      this(
            enumValue.getTitle(),
            enumValue.getReadableValues(),
            enumValue.getAllowedValues(),
            isMultipleSelect,
            horizontalLayout,
            listOnLeft);
   }
   
   public SelectWidget(PrefValue<String> prefValue,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft)
   {
      this(
            (EnumValue) prefValue, 
            isMultipleSelect,
            horizontalLayout,
            listOnLeft);
   }
   

   public SelectWidget(String title,
                       EnumValue enumValue,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft)
   {
      this(
            title,
            enumValue.getReadableValues(),
            enumValue.getAllowedValues(),
            isMultipleSelect,
            horizontalLayout,
            listOnLeft);
   }
   
   public SelectWidget(String title,
                       PrefValue<String> prefValue,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft)
   {
      this(
            title,
            (EnumValue) prefValue,
            isMultipleSelect,
            horizontalLayout,
            listOnLeft);
   }
   

   public SelectWidget(String label)
   {
      this(label, null, false);
   }

   public SelectWidget(String label, String[] options)
   {
      this(label, options, false);
   }

   public SelectWidget(String label, String[] options, boolean listOnLeft)
   {
      this(label, options, null, false, true, listOnLeft);
   }

   public SelectWidget(String label,
                       String[] options,
                       String[] values)
   {
      this(label, options, values, false, false, false);
   }

   public SelectWidget(String label,
                       String[] options,
                       String[] values,
                       boolean isMultipleSelect)
   {
      this(label, options, values, isMultipleSelect, false, false);
   }

   public SelectWidget(String label,
                       String[] options,
                       String[] values,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft)
   {
      this(
            label,
            ElementIds.SelectWidgetId.DEFAULT,
            options,
            values,
            isMultipleSelect,
            horizontalLayout,
            listOnLeft,
            false);
   }

   /**
    * @param label label text, or empty string (supplied later via setLabel), or ExternalLabel if
    *              a label will be associated outside this control
    * @param uniqueId
    * @param options
    * @param values
    * @param isMultipleSelect
    * @param horizontalLayout
    * @param listOnLeft
    * @param fillContainer
    */
   public SelectWidget(String label,
                       ElementIds.SelectWidgetId uniqueId,
                       String[] options,
                       String[] values,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft,
                       boolean fillContainer)
   {
      if (values == null)
         values = options;
      
      label = StringUtil.ensureColonSuffix(label);
      listBox_ = new ListBox();
      listBox_.setMultipleSelect(isMultipleSelect);

      // set the element ID if one is provided, otherwise let it get auto generated
      if (uniqueId != ElementIds.SelectWidgetId.DEFAULT)
      {
         uniqueId_ = "_" + uniqueId;
         ElementIds.assignElementId(listBox_, ElementIds.SELECT_WIDGET_LIST_BOX + uniqueId_);
      }

      if (options == null)
      {
         listBox_.addItem(constants_.selectWidgetListBoxNone(), constants_.selectWidgetListBoxNone());
      }
      else
      {
         for (int i = 0; i < options.length; i++)
            listBox_.addItem(options[i], values[i]);
      }

      Panel panel = null;
      if (horizontalLayout)
      {
         horizontalPanel_ = new HorizontalPanel();
         if (label != ExternalLabel)
         {
            label_ = new FormLabel(label, listBox_);
         }
         else
         {
            label_ = new FormLabel(""); // to maintain layout
         }
         if (listOnLeft)
         {
            horizontalPanel_.add(listBox_);
            horizontalPanel_.add(label_);
         }
         else
         {
            horizontalPanel_.add(label_);
            horizontalPanel_.add(listBox_);
         }

         horizontalPanel_.setCellVerticalAlignment(label_, HasVerticalAlignment.ALIGN_MIDDLE);
         horizontalPanel_.setCellVerticalAlignment(listBox_, HasVerticalAlignment.ALIGN_MIDDLE);
         panel = horizontalPanel_;
      }
      else
      {
         if (label != ExternalLabel)
         {
            label_ = new FormLabel(label, listBox_, true);
         }
         else
         {
            label_ = new FormLabel("", true); // to maintain layout
         }
         
         label_.getElement().getStyle().setMarginLeft(2, Unit.PX);
         label_.getElement().getStyle().setMarginBottom(2, Unit.PX);
         flowPanel_ = new FlowPanel();
         flowPanel_.add(label_);
         flowPanel_.add(listBox_);
         panel = flowPanel_;
      }
      
      if (!StringUtil.isNullOrEmpty(uniqueId_))
         ElementIds.assignElementId(label_, ElementIds.SELECT_WIDGET_LABEL + uniqueId_);

      initWidget(panel);

      if (fillContainer)
      {
         if (StringUtil.isNullOrEmpty(label))
            listBox_.setWidth("100%");
         horizontalPanel_.setWidth("100%");
      }

      addStyleName(ThemeResources.INSTANCE.themeStyles().selectWidget());
   }

   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return listBox_.addChangeHandler(handler);
   }

   public FormLabel getLabel()
   {
      return label_;
   }

   public ListBox getListBox()
   {
      return listBox_;
   }

   public void setLabel(String label)
   {
      label_.setText(label);
   }

   public void setChoices(String[] options)
   {
      setChoices(options, options);
   }

   public void setChoices(String[] options, String[] values)
   {
      listBox_.clear();
      for (int i = 0; i < options.length; i++)
         addChoice(options[i], values[i]);

      selectFirstItem();
   }

   public void addChoice(String option)
   {
      addChoice(option, option);
   }

   public void addChoice(String option, String value)
   {
      listBox_.addItem(option, value);
   }

   public void selectFirstItem()
   {
      if (listBox_.getItemCount() > 0)
         listBox_.setSelectedIndex(0);
   }

   public void setEnabled(boolean enabled)
   {
      listBox_.setEnabled(enabled);
   }

   public boolean isEnabled()
   {
      return listBox_.isEnabled();
   }

   public boolean setValue(String value)
   {
      for (int i = 0; i < listBox_.getItemCount(); i++)
         if (value == listBox_.getValue(i))
         {
            listBox_.setSelectedIndex(i);
            return true;
         }
      return false;
   }

   public String getValue()
   {
      if (listBox_.getSelectedIndex() < 0)
         return null;
      return listBox_.getValue(listBox_.getSelectedIndex());
   }

   public List<String> getValues()
   {
      List<String> values = new ArrayList<String>();
      
      for (int i = 0, n = listBox_.getItemCount(); i < n; i++)
      {
         if (listBox_.isItemSelected(i))
         {
            values.add(listBox_.getItemText(i));
         }
      }
      
      return values;
   }
   
   public int getIntValue()
   {
      return Integer.parseInt(getValue());
   }

   public void setIntValue(int value)
   {
      setValue(Integer.valueOf(value).toString());
   }

   public void addWidget(Widget widget)
   {
      if (horizontalPanel_ != null)
      {
         horizontalPanel_.add(widget);
         horizontalPanel_.setCellVerticalAlignment(
               widget,
               HasVerticalAlignment.ALIGN_MIDDLE);
      }
      else
      {
         flowPanel_.add(widget);
      }
   }

   public void insertValue(int index, String label, String value)
   {
      listBox_.insertItem(label, value, index);
   }

   @Override
   public void setElementId(String id)
   {
      listBox_.getElement().setId(id);
      label_.setFor(id);
   }

   public void setDescribedBy(String id)
   {
      Roles.getListboxRole().setAriaDescribedbyProperty(listBox_.getElement(), Id.of(id));
   }
   
   public void insertInto(Grid grid, int row)
   {
      addStyleName(ThemeStyles.INSTANCE.gridSelectWidget());
      grid.setWidget(row, 0, label_);
      grid.setWidget(row, 1, this);
   }

   private String uniqueId_;
   private HorizontalPanel horizontalPanel_ = null;
   private FlowPanel flowPanel_ = null;
   private FormLabel label_ = null;
   private final ListBox listBox_;
   
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
