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
    *    region (or doesn't fit below), so it doesn't cover the editor's
    *    toolbar or tabs. Only used when not preferring the bottom.
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

      Rectangle window = getWindowBounds();
      int viewportTop = viewport_ == null ? window.getTop() : viewport_.getTop();

      placedBelow_ = isPlacedBelow(
            popupHeight, cursorBounds_, preferBottom_, viewportTop, window);

      int top = placedBelow_
            ? cursorBounds_.getBottom() + GAP
            : topAbove(popupHeight, cursorBounds_);
      int left = constrainLeft(popupWidth, cursorBounds_.getLeft(), GAP);

      popup_.setPopupPosition(left, top);
   }

   /**
    * Whether a popup of the given height goes below the cursor; see
    * placeBelow.
    */
   static boolean isPlacedBelow(int popupHeight,
                                Rectangle cursorBounds,
                                boolean preferBottom,
                                int viewportTop,
                                Rectangle window)
   {
      return placeBelow(
            topAbove(popupHeight, cursorBounds),
            cursorBounds.getBottom() + GAP + popupHeight,
            preferBottom,
            viewportTop,
            window);
   }

   /**
    * Whether a popup goes below its line rather than above, given where its
    * top would be above the line and its bottom below it. Preferring the
    * bottom, it goes there if it fits in the window. Preferring the top, it
    * goes there if it fits below the viewport's top, such as the top of an
    * editor's text; failing that, it goes to whichever side fits in the
    * window, and below if neither does.
    */
   public static boolean placeBelow(int topAbove,
                                    int bottomBelow,
                                    boolean preferBottom,
                                    int viewportTop,
                                    Rectangle window)
   {
      boolean fitsBelow = bottomBelow < window.getBottom();
      if (preferBottom)
         return fitsBelow;
      if (topAbove >= Math.max(viewportTop, window.getTop()))
         return false;
      return fitsBelow || topAbove < window.getTop();
   }

   public static Rectangle getWindowBounds()
   {
      return new Rectangle(
            Window.getScrollLeft(),
            Window.getScrollTop(),
            Window.getClientWidth(),
            Window.getClientHeight());
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

   /**
    * Positions a popup below the point (pageX, pageY), the bottom of what it
    * belongs to, or above it if it doesn't fit below in the window.
    */
   public static Coordinates getPopupPosition(int width,
                                              int height,
                                              int pageX,
                                              int pageY,
                                              int fudgeFactor)
   {
      int topAbove = pageY - height - fudgeFactor - LINE_CLEARANCE;
      int topBelow = pageY + fudgeFactor;
      Rectangle window = getWindowBounds();
      boolean below = placeBelow(
            topAbove, topBelow + height, true, window.getTop(), window);

      return new Coordinates(
            constrainLeft(width, pageX, fudgeFactor),
            below ? topBelow : topAbove);
   }

   /**
    * The left edge for a popup at pageX, nudged left if the popup would
    * overflow the window on the right.
    */
   public static int constrainLeft(int width, int pageX, int fudgeFactor)
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
      setPopupPosition(panel, pageX, pageY, 0);
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
   // Extra room left between a popup above its line and the anchor it is
   // placed from, the bottom (or for a tooltip, the middle) of that line.
   public static final int LINE_CLEARANCE = 20;
   
}
