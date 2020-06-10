/*
 * PopupPositioner.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import org.rstudio.core.client.Rectangle;

public class PopupPositioner implements PositionCallback
{
   private Rectangle cursorBounds_;
   private CompletionPopupDisplay popup_;
   
   public PopupPositioner(Rectangle cursorBounds, CompletionPopupDisplay popup)
   {
      this.cursorBounds_ = cursorBounds;
      popup_ = popup;
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
            5,
            true);
      
      popup_.setPopupPosition(coords.getLeft(), coords.getTop());
   }
   
   public static class Coordinates
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
                                              int fudgeFactor,
                                              boolean preferBottom)
   {
      int windowTop = Window.getScrollTop();
      int windowLeft = Window.getScrollLeft();
      int windowRight = windowLeft + Window.getClientWidth();
      int windowBottom = windowTop + Window.getClientHeight();
      
      // Check to see if the popup would overflow to the right.
      // If so, nudge the coordinates left to prevent this.
      int horizontalOverflow = pageX + width - windowRight;
      if (horizontalOverflow > fudgeFactor)
      {
         pageX = Math.max(
               fudgeFactor + 10,
               pageX - horizontalOverflow);
      }
      
      // Compute the vertical position. Normally we want the
      // completion popup to appear below the rectangle, but
      // we may need to position it above (e.g. R completions
      // in the console).
      if (preferBottom)
      {
         boolean showOnBottom = pageY + height + fudgeFactor < windowBottom;
         pageY = showOnBottom
               ? pageY + fudgeFactor
               : pageY - height - fudgeFactor - 10;
      }
      else
      {
         boolean showOnTop = pageY - height - fudgeFactor > 0;
         pageY = showOnTop
               ? pageY - height - fudgeFactor - 10
               : pageY + fudgeFactor;
      }
      
      return new Coordinates(pageX, pageY);
   }
   
   public static void setPopupPosition(PopupPanel panel,
                                       int pageX,
                                       int pageY)
   {
      setPopupPosition(panel, pageX, pageY, 0, true);
   }
   
   
   public static void setPopupPosition(PopupPanel panel,
                                       int pageX,
                                       int pageY,
                                       int fudgeFactor)
   {
      setPopupPosition(panel, pageX, pageY, fudgeFactor, true);
   }
   
   public static void setPopupPosition(PopupPanel panel,
                                       int pageX,
                                       int pageY,
                                       int fudgeFactor,
                                       boolean preferBottom)
   {
      Coordinates transformed = getPopupPosition(
            panel.getOffsetWidth(),
            panel.getOffsetHeight(),
            pageX,
            pageY,
            fudgeFactor,
            preferBottom);
      
      panel.setPopupPosition(
            transformed.getLeft(),
            transformed.getTop());
   }
}
