/*
 * DoubleClickState.java
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

import com.google.gwt.dom.client.NativeEvent;
import org.rstudio.core.client.Point;

import java.util.Date;

/**
 * Helper class to make it easy to detect double-clicks in click handlers.
 */
public class DoubleClickState
{
   public boolean checkForDoubleClick(NativeEvent event)
   {
      if (event.getButton() != NativeEvent.BUTTON_LEFT)
      {
         lastClickPos_ = null;
         lastClickTime_ = null;
         return false;
      }

      Date now = new Date();

      if (!isDoubleClick(event, now))
      {
         lastClickPos_ = Point.create(event.getClientX(), event.getClientY());
         lastClickTime_ = now;
         return false;
      }
      else
      {
         // Prevent three clicks from generating two double clicks
         lastClickPos_ = null;
         lastClickTime_ = null;
         return true;
      }
   }

   private boolean isDoubleClick(NativeEvent event, Date now)
   {
      if (lastClickPos_ == null || lastClickTime_ == null)
         return false;

      long millisSinceLast = now.getTime() - lastClickTime_.getTime();
      if (millisSinceLast > 500)
         return false;

      if (Math.abs(lastClickPos_.x - event.getClientX()) > 3
          || Math.abs(lastClickPos_.y - event.getClientY()) > 3)
         return false;

      return true;
   }

   private Point lastClickPos_;
   private Date lastClickTime_;
}
