/*
 * GlassAttacher.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Similar to GlassPanel, except it doesn't introduce its own LayoutPanel but
 * rather re-uses an existing LayoutPanel. We have found that excessively deeply
 * element nesting is causing performance problems for Ace on some platforms
 * (specifically desktop on Xubuntu 12.04).
 */
public class GlassAttacher
{
   public GlassAttacher(LayoutPanel panel)
   {
      panel_ = panel;
      glass_ = createGlassWidget();
   }

   public void setGlass(boolean on)
   {
      if (on)
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

   private static Widget createGlassWidget()
   {
      HTML glass = new HTML();
      glass.setSize("100%", "100%");
      Element glassElem = glass.getElement();
      glassElem.getStyle().setBackgroundColor("white");
      glassElem.getStyle().setProperty("opacity", "0.0");
      glassElem.getStyle().setProperty("filter", "alpha(opacity=0)");
      return glass;
   }

   private final LayoutPanel panel_;
   private final Widget glass_;
}
