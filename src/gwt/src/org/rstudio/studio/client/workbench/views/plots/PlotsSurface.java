/*
 * PlotsSurface.java
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
package org.rstudio.studio.client.workbench.views.plots;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;

public class PlotsSurface extends FlowPanel
{
   PlotsSurface(LayoutPanel parent)
   {
      parent_ = parent;
   }

   public void enableSurface()
   {
      setSize("100%", "100%");
      parent_.setWidgetVisible(this, true);
      parent_.setWidgetTopBottom(this, 0, Unit.PX, 0, Unit.PX);
      parent_.setWidgetLeftRight(this, 0, Unit.PX, 0, Unit.PX);
   }

   public void disableSurface()
   {
      setSize("0%", "0%");
      parent_.setWidgetVisible(this, false);
      parent_.setWidgetTopBottom(this, 0, Unit.PX, 100, Unit.PCT);
      parent_.setWidgetLeftRight(this, 0, Unit.PX, 100, Unit.PCT);
   }

   private final LayoutPanel parent_;
}
