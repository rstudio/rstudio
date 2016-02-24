/*
 * TextBoxWithPrefix.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class TextBoxWithPrefix extends Composite
{
   public TextBoxWithPrefix()
   {
      HorizontalPanel panel = new HorizontalPanel();

      prefixLabel_ = new Label();
      panel.add(prefixLabel_);
      textBox_ = new TextBox();
      panel.add(textBox_);
      
      // draw a border around the panel and remove it from the textbox
      textBox_.getElement().getStyle().setBorderWidth(0, Unit.PX);
      textBox_.getElement().getStyle().setOutlineWidth(0, Unit.PX);
      textBox_.setWidth("100%");
      textBox_.setHeight("100%");
      panel.getElement().getStyle().setBackgroundColor("#FFFFFF");
      panel.getElement().getStyle().setBorderColor("#999999");
      panel.getElement().getStyle().setBorderStyle(BorderStyle.SOLID);
      panel.getElement().getStyle().setBorderWidth(1, Unit.PX);
      panel.setCellWidth(prefixLabel_, "1");
      
      panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      
      initWidget(panel);
   }
   
   public void setFocus(boolean focused)
   {
      textBox_.setFocus(focused);
   }
   
   public void setPrefix(String prefix)
   {
      prefixLabel_.setText(prefix);
   }
   
   public String getText()
   {
      return prefixLabel_.getText() + textBox_.getText();
   }
   
   private Label prefixLabel_;
   private TextBox textBox_;
}
