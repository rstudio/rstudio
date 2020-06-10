/*
 * DockPanelSidebarDragHandler.java
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

import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.events.MouseDragHandler;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;


public abstract class DockPanelSidebarDragHandler extends MouseDragHandler
{
   public DockPanelSidebarDragHandler(DockLayoutPanel panel, Widget sidebar)
   {
      panel_ = panel;
      sidebar_ = sidebar;
   }
   
   @Override
   public boolean beginDrag(MouseDownEvent event)
   {
      initialWidth_ = panel_.getWidgetSize(sidebar_);
      return true;
   }
   
   @Override
   public void onDrag(MouseDragEvent event)
   {
      double initialWidth = initialWidth_;
      double xDiff = event.getTotalDelta().getMouseX();
      double newSize = initialWidth - xDiff;
      
      // We allow an extra pixel here just to 'hide' the border
      // if the outline is maximized, since the 'separator'
      // lives as part of the outline instead of 'between' the
      // two widgets
      double maxSize = panel_.getOffsetWidth() + 1;
      
      double clamped = MathUtil.clamp(newSize, 0, maxSize);
      
      // If the size is below '5px', interpret this as a request
      // to close the outline widget.
      if (clamped < 5)
         clamped = 0;
      
      panel_.setWidgetSize(sidebar_, clamped);
      
      onResized(clamped != 0);
     
   }
   
   @Override
   public void endDrag()
   {
      double size = panel_.getWidgetSize(sidebar_);
      
      // We only update the preferred size if the user hasn't closed
      // the widget.
      if (size > 0)
         onPreferredWidth(size);
      
      onPreferredVisibility(size > 0);
   }
   

   public abstract void onResized(boolean visible);
   public void onPreferredWidth(double size) {}
   public void onPreferredVisibility(boolean visible) {}
   
   final Widget sidebar_;
   final DockLayoutPanel panel_;
   double initialWidth_ = 0;
}

      