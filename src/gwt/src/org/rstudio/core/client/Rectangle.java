/*
 * Rectangle.java
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
package org.rstudio.core.client;

public class Rectangle
{
   public Rectangle(int x, int y, int width, int height)
   {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   // Eclipse auto-generated
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + height;
      result = prime * result + width;
      result = prime * result + x;
      result = prime * result + y;
      return result;
   }

   // Eclipse auto-generated
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Rectangle other = (Rectangle) obj;
      if (height != other.height)
         return false;
      if (width != other.width)
         return false;
      if (x != other.x)
         return false;
      return y == other.y;
   }

   @Override
   public String toString()
   {
      return "Rectangle(x=" + x + ",y=" + y +
             ",w=" + width + ",h=" + height + ")";
   }

   public int getLeft()
   {
      return x;
   }
   
   public int getTop()
   {
      return y;
   }
   
   public int getWidth()
   {
      return width;
   }
   
   public int getHeight()
   {
      return height;
   }
   
   public int getRight()
   {
      return x + width;
   }
   
   public int getBottom()
   {
      return y + height;
   }
   
   public Point getLocation()
   {
      return Point.create(x, y);
   }
   
   public Size getSize()
   {
      return new Size(width, height);
   }
   
   public Point getCorner(boolean left, boolean top)
   {
      return Point.create(
            left ? getLeft() : getRight(),
            top ? getTop() : getBottom());
   }

   public Rectangle move(int x, int y)
   {
      return new Rectangle(x, y, getWidth(), getHeight());
   }

   /**
    * Returns the rectangular intersection of the two rectangles, or null if
    * the rectangles do not touch anywhere.
    * @param other The rectangle to intersect with this.
    * @param canReturnEmptyRect If true, in cases where one of the
    *    rectangles has zero width or height OR cases where the rectangles share
    *    an edge, it's possible for a zero-width or zero-height rectangle to be
    *    returned. If false, then it's guaranteed that the return value will
    *    either be null or a rectangle with a positive area. Note that
    *    regardless of true or false, null can always be returned.
    * @return The intersection, or null if none.
    */
   public Rectangle intersect(Rectangle other, boolean canReturnEmptyRect)
   {
      int left = Math.max(x, other.x);
      int right = Math.min(getRight(), other.getRight());
      int top = Math.max(getTop(), other.getTop());
      int bottom = Math.min(getBottom(), other.getBottom());

      if ((canReturnEmptyRect && left <= right && top <= bottom)
          || (!canReturnEmptyRect && left < right && top < bottom))
      {
         return new Rectangle(left, top, right-left, bottom-top);
      }
      else
         return null;
   }

   /**
    * Enlarge the rectangle in the given directions by the given amounts.
    */
   public Rectangle inflate(int left, int top, int right, int bottom)
   {
      return new Rectangle(x - left, y - top,
                           width + left + right, height + top + bottom);
   }

   /**
    * Enlarge each side of the rectangle by the given amount. Note that e.g.
    * 10 will result in 20-unit-greater width and height.
    * @param inflateBy
    * @return
    */
   public Rectangle inflate(int inflateBy)
   {
      return inflate(inflateBy, inflateBy, inflateBy, inflateBy);
   }

   /**
    * Return the center point
    */
   public Point center()
   {
      return Point.create(
            (getLeft() + getRight()) / 2,
            (getTop() + getBottom()) / 2);
   }

   /**
    * Create a new rectangle of the given width and height that is centered
    * relative to this rectangle.
    */
   public Rectangle createCenteredRect(int width, int height)
   {
      return new Rectangle(
            (getWidth() - width) / 2,
            (getHeight() - height) / 2,
            width,
            height);
   }

   /**
    * Returns true if this rectangle ENTIRELY contains the given rectangle.
    */
   public boolean contains(Rectangle other)
   {
      return getLeft() <= other.getLeft() &&
             getTop() <= other.getTop() &&
             getRight() >= other.getRight() &&
             getBottom() >= other.getBottom();
   }

   /**
    * Intelligently figures out where to move this rectangle within a container
    * to avoid another rectangle (the avoidee).
    * @param avoidee The rectangle we're trying to avoid
    * @param container The rectangle we need to try to stay within, if possible
    * @return The new location for the rectangle
    */
   public Point avoidBounds(final Rectangle avoidee,
                            final Rectangle container)
   {
      // Check for nothing to avoid
      if (avoidee == null)
         return this.getLocation();

      // Check for no collision
      if (this.intersect(avoidee, false) == null)
         return this.getLocation();

      // Figure out whether the avoidee is in the top or bottom half of the
      // container. vertDir < 0 means top half, vertDir > 0 means bottom half.
      int vertDir = avoidee.center().getY() - container.center().getY();
      // Create new bounds that are just below or just above the avoidee,
      // depending on whether the avoidee is in the top or bottom half of
      // the container, respectively.
      Rectangle vertShift = this.move(
            this.getLeft(),
            vertDir > 0 ? avoidee.getTop() - this.getHeight()
                        : avoidee.getBottom());
      // If that resulted in bounds that fit entirely in the container, then
      // use it. (We prefer vertical shifting to horizontal shifting.)
      if (container.contains(vertShift))
         return vertShift.getLocation();

      // Now repeat the algorithm in the horizontal dimension.
      int horizDir = avoidee.center().getX() - container.center().getX();
      Rectangle horizShift = this.move(
            horizDir > 0 ? avoidee.getLeft() - this.getWidth()
                         : avoidee.getRight(),
            this.getTop());
      if (container.contains(horizShift))
         return horizShift.getLocation();

      // Both vertical and horizontal options go off the container. Combine
      // their effects, then move to within the screen, if possible; or if all
      // else fails, just center.
      Rectangle hvShift = new Rectangle(horizShift.getLeft(),
                                        vertShift.getTop(),
                                        this.getWidth(),
                                        this.getHeight());
      return hvShift.attemptToMoveInto(container,
                                       FailureMode.CENTER).getLocation();
   }

   public enum FailureMode
   {
      /**
       * Center the rect's position in this dimension
       */
      CENTER,
      /**
       * Don't change the rect's position in this dimension
       */
      NO_CHANGE
   }

   /**
    * Attempt to move this rectangle so that it fits inside the given container.
    * If this rectangle is taller and/or wider than the container, then the
    * failureMode parameter can be used to dictate what the fallback behavior
    * should be.
    */
   public Rectangle attemptToMoveInto(Rectangle container,
                                      FailureMode failureMode)
   {
      int newX = this.x;
      int newY = this.y;

      if (getWidth() <= container.getWidth())
      {
         newX = Math.min(Math.max(newX, container.getLeft()),
                         container.getRight() - getWidth());
      }
      else if (failureMode == FailureMode.CENTER)
      {
         newX = container.getLeft() - (getWidth()-container.getWidth())/2;
      }

      if (getHeight() <= container.getHeight())
      {
         newY = Math.min(Math.max(newY, container.getTop()),
                         container.getBottom() - getHeight());
      }
      else if (failureMode == FailureMode.CENTER)
      {
         newY = container.getTop() - (getHeight()-container.getHeight())/2;
      }

      return new Rectangle(newX, newY, getWidth(), getHeight());
   }

   private final int x;
   private final int y;
   private final int width;
   private final int height;
}
