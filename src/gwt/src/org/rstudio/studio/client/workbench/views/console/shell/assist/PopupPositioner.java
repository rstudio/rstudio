/*
 * PopupPositioner.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import org.rstudio.core.client.Rectangle;

public class PopupPositioner implements PositionCallback
{
   private Rectangle cursorBounds_ ;
   private CompletionPopupDisplay popup_ ;
   
   public PopupPositioner(Rectangle cursorBounds, CompletionPopupDisplay popup)
   {
      this.cursorBounds_ = cursorBounds ;
      popup_ = popup ;
   }

   public void setPosition(int popupWidth, int popupHeight)
   {
      if (cursorBounds_ == null)
      {
         assert false : "Positioning popup but no cursor bounds available";
         return;
      }
      
      Coordinates coords = getPopupPosition(
            popupWidth,
            popupHeight,
            cursorBounds_.getLeft(),
            cursorBounds_.getBottom(),
            5);
      
      popup_.setPopupPosition(coords.getLeft(), coords.getTop());
   }
   
   private static class Coordinates
   {
      public Coordinates(int left, int top)
      {
         left_ = left;
         top_ = top;
      }
      
      public int getLeft() { return left_; }
      public int getTop() { return top_; }
      
      private final int left_;
      private final int top_;
   }
   
   public static Coordinates getPopupPosition(int width,
                                              int height,
                                              int pageX,
                                              int pageY,
                                              int fudgeFactor)
   {
      int windowTop = Window.getScrollTop();
      int windowLeft = Window.getScrollLeft();
      int windowRight = windowLeft + Window.getClientWidth();
      int windowBottom = windowTop + Window.getClientHeight();
      
      // Compute the horizontal position.
      int left = pageX + fudgeFactor;
      
      // Check to see if the popup would overflow to the right.
      // If so, nudge the coordinates left to prevent this.
      int horizontalOverflow = pageX + width + fudgeFactor - windowRight;
      if (horizontalOverflow > 0)
      {
         left = Math.max(
               fudgeFactor + 10,
               left - horizontalOverflow);
      }
      
      // Compute the vertical position. Normally we want the
      // completion popup to appear below the rectangle, but
      // we may need to position it above (e.g. R completions
      // in the console).
      boolean showOnBottom =
            pageY + height + fudgeFactor < windowBottom;
      
      int top = showOnBottom ?
            pageY + fudgeFactor :
            pageY - height - fudgeFactor - 10;
      
      return new Coordinates(left, top);
   }
   
   public static void setPopupPosition(PopupPanel panel,
                                       int pageX,
                                       int pageY,
                                       int fudgeFactor)
   {
      Coordinates transformed = getPopupPosition(
            panel.getOffsetWidth(),
            panel.getOffsetHeight(),
            pageX,
            pageY,
            fudgeFactor);
      
      panel.setPopupPosition(
            transformed.getLeft(),
            transformed.getTop());
   }
}