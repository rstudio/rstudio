/*
 * FixedRatioWidget.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FixedRatioWidget extends Composite
{
   public FixedRatioWidget(Widget widget, double aspect, int maxWidth)
   {
      widget_ = widget;

      HTMLPanel outer = new HTMLPanel("");
      Style outerStyle = outer.getElement().getStyle();
      outerStyle.setWidth(100, Unit.PCT);
      outerStyle.setProperty("maxWidth", maxWidth + "px");
      
      HTMLPanel panel = new HTMLPanel("");
      Style panelStyle = panel.getElement().getStyle();
      panelStyle.setPosition(Position.RELATIVE);
      panelStyle.setWidth(100, Unit.PCT);
      panelStyle.setHeight(0, Unit.PX);
      panelStyle.setPaddingBottom((double)100 * ((double)1/aspect), 
            Unit.PCT);
      outer.add(panel);
      
      Style widgetStyle = widget.getElement().getStyle();
      widgetStyle.setPosition(Position.ABSOLUTE);
      widgetStyle.setWidth(100, Unit.PCT);
      widgetStyle.setHeight(100, Unit.PCT);
      widgetStyle.setLeft(0, Unit.PX);
      widgetStyle.setTop(0, Unit.PX);
      
      panel.add(widget_);
      initWidget(outer);
   }
   
   public Widget getWidget()
   {
      return widget_;
   }
   
   private final Widget widget_;
}
