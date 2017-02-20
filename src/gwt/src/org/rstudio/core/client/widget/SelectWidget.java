/*
 * SelectWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class SelectWidget extends Composite
{
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
      this(label, options, values, isMultipleSelect, 
           horizontalLayout, listOnLeft, false);
   }
   
   public SelectWidget(String label,
                       String[] options,
                       String[] values,
                       boolean isMultipleSelect,
                       boolean horizontalLayout,
                       boolean listOnLeft,
                       boolean fillContainer)
   {
      if (values == null)
         values = options;

      listBox_ = new ListBox();
      listBox_.setMultipleSelect(isMultipleSelect);
      if (options == null)
      {
         listBox_.addItem("(None)", "(None)");
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
         Label labelWidget = new Label(label);
         if (listOnLeft)
         {
            horizontalPanel_.add(listBox_);
            horizontalPanel_.add(labelWidget);
         }
         else
         {
            horizontalPanel_.add(labelWidget);
            horizontalPanel_.add(listBox_);
         }
        
         horizontalPanel_.setCellVerticalAlignment(
                                          labelWidget, 
                                          HasVerticalAlignment.ALIGN_MIDDLE);
         panel = horizontalPanel_;
      }
      else
      {
         flowPanel_ = new FlowPanel();
         flowPanel_.add(new Label(label, true));
         panel = flowPanel_;
         panel.add(listBox_);
      }
      
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

   public ListBox getListBox()
   {
      return listBox_;
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
      
      if (listBox_.getItemCount() > 0)
         listBox_.setSelectedIndex(0);
   }
   
   public void addChoice(String option, String value)
   {
      listBox_.addItem(option, value);
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
         if (value.equals(listBox_.getValue(i)))
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

   public int getIntValue()
   {
      return Integer.parseInt(getValue());
   }
   
   public void setIntValue(int value)
   {
      setValue(new Integer(value).toString());
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
   
   private HorizontalPanel horizontalPanel_ = null;
   private FlowPanel flowPanel_ = null;
   private final ListBox listBox_;
}
