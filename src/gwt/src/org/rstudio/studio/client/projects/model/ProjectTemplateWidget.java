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

import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithCue;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
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
         collector_.collectInput(receiver);
      }
      
      private final Collector collector_;
   }
   
   public ProjectTemplateWidget(ProjectTemplateDescription description)
   {
      description_ = description;
      widgets_ = new ArrayList<ProjectTemplateWidgetItem>();
      
      // initialize widgets
      JsArray<ProjectTemplateWidgetDescription> widgets = description.getWidgetDescription();
      int n = widgets.length();
      for (int i = 0; i < n; i++)
         widgets_.add(createWidget(widgets.get(i)));
      
      // initialize panel
      VerticalPanel panel = new VerticalPanel();
      for (int i = 0; i < n; i++)
         panel.add(widgets_.get(i));
      
      initWidget(panel);
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
      if (type.equals("checkbox"))
         return checkBoxInput(description);
      else if (type.equals("selectbox"))
         return selectBoxInput(description);
      else if (type.equals("textinput"))
         return textInput(description);
      else if (type.equals("fileinput"))
         return fileInput(description);
      
      return null;
   }
   
   private ProjectTemplateWidgetItem checkBoxInput(final ProjectTemplateWidgetDescription description)
   {
      final CheckBox widget = new CheckBox(description.getLabel());
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
      final SelectWidget widget = new SelectWidget(description.getLabel(), fields);
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
      final TextBoxWithCue widget = new TextBoxWithCue(description.getLabel());
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
   
   private ProjectTemplateWidgetItem fileInput(final ProjectTemplateWidgetDescription description)
   {
      final FileChooserTextBox widget = new FileChooserTextBox(description.getLabel(), null);
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
   
   private final ProjectTemplateDescription description_;
   private final List<ProjectTemplateWidgetItem> widgets_;
}
