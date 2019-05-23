/*
 * ProjectTemplateWidget.java
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
package org.rstudio.studio.client.projects.model;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.SelectWidget;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ProjectTemplateWidget extends Composite
{
   private interface Collector
   {
      public void collectInput(JsObject receiver);
   }
   
   private static class ProjectTemplateWidgetItem extends Composite
   {
      public ProjectTemplateWidgetItem(Widget widget, Collector collector)
      {
         collector_ = collector;
         initWidget(widget);
      }
      
      public void collectInput(JsObject receiver)
      {
         if (collector_ != null)
            collector_.collectInput(receiver);
      }
      
      private final Collector collector_;
   }
   
   public ProjectTemplateWidget(ProjectTemplateDescription description)
   {
      widgets_ = new ArrayList<ProjectTemplateWidgetItem>();
      
      // initialize widgets
      JsArray<ProjectTemplateWidgetDescription> descriptions = description.getWidgetDescription();
      int n = descriptions.length();
      for (int i = 0; i < n; i++)
         widgets_.add(createWidget(descriptions.get(i)));
      
      // initialize panel
      VerticalPanel leftPanel = new VerticalPanel();
      VerticalPanel rightPanel = new VerticalPanel();
      for (int i = 0; i < n; i++)
      {
         String position = StringUtil.notNull(descriptions.get(i).getPosition());
         Widget widget = widgets_.get(i);
         if ("right".equals(position.toLowerCase()))
            rightPanel.add(widget);
         else
            leftPanel.add(widget);
      }
      
      int nLeft = leftPanel.getWidgetCount();
      int nRight = rightPanel.getWidgetCount();
      
      Widget primaryWidget;
      
      if (nLeft == 0 && nRight == 0)
         primaryWidget = new FlowPanel();
      else if (nRight == 0)
         primaryWidget = leftPanel;
      else if (nLeft == 0)
         primaryWidget = rightPanel;
      else
      {
         // both sides have widgets -- populate appropriately
         HorizontalPanel panel = new HorizontalPanel();
         panel.add(leftPanel);
         panel.add(rightPanel);
         primaryWidget = panel;
      }
      
      primaryWidget.setWidth("100%");
      initWidget(primaryWidget);
   }
   
   public JsObject collectInput()
   {
      JsObject object = JsObject.createJsObject();
      for (ProjectTemplateWidgetItem widget : widgets_)
         widget.collectInput(object);
      return object;
   }
   
   private ProjectTemplateWidgetItem createWidget(ProjectTemplateWidgetDescription description)
   {
      String type = description.getType().toLowerCase();
      if (type.equals(TYPE_CHECKBOX_INPUT))
         return checkBoxInput(description);
      else if (type.equals(TYPE_SELECT_INPUT))
         return selectBoxInput(description);
      else if (type.equals(TYPE_TEXT_INPUT))
         return textInput(description);
      else if (type.equals(TYPE_FILE_INPUT))
         return fileInput(description);
      
      Debug.log("Unexpected widget type '" + type + "'");
      return new ProjectTemplateWidgetItem(new FlowPanel(), null);
   }
   
   private ProjectTemplateWidgetItem checkBoxInput(final ProjectTemplateWidgetDescription description)
   {
      final CheckBox widget = new CheckBox(description.getLabel());
      
      // set default value
      String defaultValue = description.getDefault();
      if (!StringUtil.isNullOrEmpty(defaultValue))
         widget.setValue(isTruthy(defaultValue));
      
      return new ProjectTemplateWidgetItem(widget, new Collector()
      {
         @Override
         public void collectInput(JsObject receiver)
         {
            boolean value = widget.getValue();
            receiver.setBoolean(description.getParameter(), value);
         }
      });
   }
   
   private ProjectTemplateWidgetItem selectBoxInput(final ProjectTemplateWidgetDescription description)
   {
      String[] fields = readSelectBoxFields(description);
      String label = ensureEndsWithColon(description.getLabel());
      final SelectWidget widget = new SelectWidget(label, fields);
      
      String defaultValue = description.getDefault();
      if (!StringUtil.isNullOrEmpty(defaultValue))
         widget.setValue(defaultValue);
      
      return new ProjectTemplateWidgetItem(widget, new Collector()
      {
         @Override
         public void collectInput(JsObject receiver)
         {
            String value = widget.getValue();
            receiver.setString(description.getParameter(), value);
         }
      });
   }
   
   private ProjectTemplateWidgetItem textInput(final ProjectTemplateWidgetDescription description)
   {
      final TextBox primaryWidget = new TextBox();
      primaryWidget.setWidth("180px");
      
      String defaultValue = description.getDefault();
      if (!StringUtil.isNullOrEmpty(defaultValue))
         primaryWidget.setText(defaultValue);
      
      Grid grid = new Grid(1, 2);
      primaryWidget.getElement().setAttribute("spellcheck", "false");
      grid.setWidget(0, 0, new Label(ensureEndsWithColon(description.getLabel())));
      grid.setWidget(0, 1, primaryWidget);
      
      return new ProjectTemplateWidgetItem(grid, new Collector()
      {
         @Override
         public void collectInput(JsObject receiver)
         {
            String value = primaryWidget.getValue();
            receiver.setString(description.getParameter(), value);
         }
      });
   }
   
   private ProjectTemplateWidgetItem fileInput(final ProjectTemplateWidgetDescription description)
   {
      final FileChooserTextBox widget = new FileChooserTextBox(description.getLabel(), null);
      
      String defaultValue = description.getDefault();
      if (!StringUtil.isNullOrEmpty(defaultValue))
         widget.setText(defaultValue);
      
      return new ProjectTemplateWidgetItem(widget, new Collector()
      {
         @Override
         public void collectInput(JsObject receiver)
         {
            String value = widget.getText();
            receiver.setString(description.getParameter(), value);
         }
      });
   }
   
   private String[] readSelectBoxFields(ProjectTemplateWidgetDescription description)
   {
      JsArrayString jsFields = description.getFields();
      int n = jsFields.length();
      
      String[] fields = new String[n];
      for (int i = 0; i < n; i++)
         fields[i] = jsFields.get(i);
      return fields;
   }
   
   private String ensureEndsWithColon(String string)
   {
      return string.endsWith(":")
            ? string
            : string + ":";
   }
   
   private boolean isTruthy(String value)
   {
      String lower = value.toLowerCase();
      return
            lower.equals("true") ||
            lower.equals("yes") ||
            lower.equals("on") ||
            lower.equals("1");
   }
   
   private final List<ProjectTemplateWidgetItem> widgets_;
   
   public static final String TYPE_CHECKBOX_INPUT = "checkboxinput";
   public static final String TYPE_SELECT_INPUT   = "selectinput";
   public static final String TYPE_FILE_INPUT     = "fileinput";
   public static final String TYPE_TEXT_INPUT     = "textinput";
   
}
