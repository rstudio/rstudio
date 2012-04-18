/*
 * GlassPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.*;

public class GlassPanel extends ResizeComposite
{
   public GlassPanel(Widget child)
   {
      child_ = child;
      panel_ = new LayoutPanel();

      panel_.add(child);
      panel_.setWidgetLeftRight(child, 0, Unit.PX, 0, Unit.PX);
      panel_.setWidgetTopBottom(child, 0, Unit.PX, 0, Unit.PX);

      glass_ = new HTML();
      glass_.setSize("100%", "100%");
      Element glassElem = glass_.getElement();
      glassElem.getStyle().setBackgroundColor("white");
      glassElem.getStyle().setProperty("opacity", "0.0");
      glassElem.getStyle().setProperty("filter", "alpha(opacity=0)");

      setGlass(false);

      initWidget(panel_);
   }

   public void setGlass(boolean enabled)
   {
      if (enabled)
      {
         if (glass_.getParent() != panel_)
         {
            panel_.add(glass_);
            panel_.setWidgetLeftRight(glass_, 0, Unit.PX, 0, Unit.PX);
            panel_.setWidgetTopBottom(glass_, 0, Unit.PX, 0, Unit.PX);
         }
      }
      else
      {
         if (glass_.getParent() == panel_)
            panel_.remove(glass_);
      }
   }
   
   public Element getChildContainerElement()
   {
      return panel_.getWidgetContainerElement(child_);
   }

   private LayoutPanel panel_;
   private Widget glass_;
   private final Widget child_;
}
