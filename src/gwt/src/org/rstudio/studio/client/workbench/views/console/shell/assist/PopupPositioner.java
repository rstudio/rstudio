/*
 * PopupPositioner.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.Rectangle;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class PopupPositioner implements PositionCallback
{
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
   
   /**
    * @param preferBottom Whether the popup goes below the cursor when it
    *    fits there, rather than above.
    * @param viewport The page region the editor's text shows in, or null.
    *    A popup that prefers the top goes above only if it fits inside this
    *    region, so it doesn't cover the editor's toolbar or tabs.
    */
   public PopupPositioner(Rectangle cursorBounds,
                          CompletionPopupDisplay popup,
                          boolean preferBottom,
                          Rectangle viewport)
   {
      cursorBounds_ = cursorBounds;
      popup_ = popup;
      preferBottom_ = preferBottom;
      viewport_ = viewport;
   }

   public PopupPositioner(Rectangle cursorBounds,
                          CompletionPopupDisplay popup)
   {
      this(cursorBounds, popup, true, null);
   }

   public void setPosition(int popupWidth, int popupHeight)
   {
      if (cursorBounds_ == null)
      {
         assert false : "Positioning popup but no cursor bounds available";
         return;
      }

      int windowTop = Window.getScrollTop();
      int aboveLimit = viewport_ == null
            ? windowTop
            : Math.max(windowTop, viewport_.getTop());
      int belowLimit = windowTop + Window.getClientHeight();

      placedBelow_ = isPlacedBelow(
            popupHeight, cursorBounds_, preferBottom_, aboveLimit, belowLimit);

      int top = placedBelow_
            ? cursorBounds_.getBottom() + GAP
            : topAbove(popupHeight, cursorBounds_);
      int left = constrainLeft(popupWidth, cursorBounds_.getLeft(), GAP);

      popup_.setPopupPosition(left, top);
   }

   /**
    * Whether a popup of the given height goes below the cursor: on its
    * preferred side when it fits between the cursor and that side's limit,
    * otherwise on the other side.
    */
   static boolean isPlacedBelow(int popupHeight,
                                Rectangle cursorBounds,
                                boolean preferBottom,
                                int aboveLimit,
                                int belowLimit)
   {
      if (preferBottom)
         return cursorBounds.getBottom() + GAP + popupHeight < belowLimit;
      return topAbove(popupHeight, cursorBounds) < aboveLimit;
   }

   // Measured up from the bounds' bottom, as the bounds can be shorter
   // than the line (see AceEditor.getPositionBounds).
   private static int topAbove(int popupHeight, Rectangle cursorBounds)
   {
      return cursorBounds.getBottom() - LINE_CLEARANCE - GAP - popupHeight;
   }

   /**
    * Whether the last call to setPosition put the popup below the cursor.
    */
   public boolean isPlacedBelow()
   {
      return placedBelow_;
   }

   public static Coordinates getPopupPosition(int width,
                                              int height,
                                              int pageX,
                                              int pageY,
                                              int fudgeFactor,
                                              boolean preferBottom)
   {
      int windowTop = Window.getScrollTop();
      int windowBottom = windowTop + Window.getClientHeight();

      pageX = constrainLeft(width, pageX, fudgeFactor);

      // Compute the vertical position. Normally we want the
      // completion popup to appear below the rectangle, but
      // we may need to position it above (e.g. R completions
      // in the console).
      if (preferBottom)
      {
         boolean showOnBottom = pageY + height + fudgeFactor < windowBottom;
         pageY = showOnBottom
               ? pageY + fudgeFactor
               : pageY - height - fudgeFactor - 20;
      }
      else
      {
         boolean showOnTop = pageY - height - fudgeFactor > 0;
         pageY = showOnTop
               ? pageY - height - fudgeFactor - 20
               : pageY + fudgeFactor;
      }
      
      return new Coordinates(pageX, pageY);
   }

   // If the popup would overflow the window on the right, nudge it left.
   private static int constrainLeft(int width, int pageX, int fudgeFactor)
   {
      int windowRight = Window.getScrollLeft() + Window.getClientWidth();
      int horizontalOverflow = pageX + width - windowRight;
      if (horizontalOverflow > fudgeFactor)
         pageX = Math.max(fudgeFactor + 10, pageX - horizontalOverflow);
      return pageX;
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
   
   public boolean getPreferBottom()
   {
      return preferBottom_;
   }
   
   private final Rectangle cursorBounds_;
   private final CompletionPopupDisplay popup_;
   private final boolean preferBottom_;
   private final Rectangle viewport_;
   private boolean placedBelow_;

   // Space between the cursor's line and the popup.
   private static final int GAP = 5;
   private static final int LINE_CLEARANCE = 20;
   
}
