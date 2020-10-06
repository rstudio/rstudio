/*
 * Locator.java
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

import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Size;

public class Locator implements HasSelectionHandlers<Point>
{
   public interface Display extends HasSelectionHandlers<Point>
   {
      void showLocator(Plots.Parent parent);
      void hideLocator();
      boolean isVisible();
   }

   public Locator(Plots.Parent parent)
   {
      parent_ = parent;
   }

   public HandlerRegistration addSelectionHandler(
         SelectionHandler<Point> handler)
   {
      return handlers_.addHandler(SelectionEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public boolean isActive()
   {
      return display_ != null;
   }

   public void locate(String url, Size size)
   {
      if (currentUrl_ == null || !currentUrl_.equals(url) ||
          currentSize_ == null || !currentSize_.equals(size) ||
          display_ == null || !display_.isVisible())
      {
         clearDisplay();

         display_ = new LocatorPanel();
         hreg_ = display_.addSelectionHandler(new SelectionHandler<Point>()
         {
            public void onSelection(SelectionEvent<Point> event)
            {
               SelectionEvent.fire(Locator.this, event.getSelectedItem());
            }
         });
         currentUrl_ = url;
         currentSize_ = size;
         display_.showLocator(parent_);
      }
   }

   public void clearDisplay()
   {
      currentUrl_ = null;
      currentSize_ = null;

      if (display_ != null)
      {
         display_.hideLocator();
         display_ = null;
      }

      if (hreg_ != null)
      {
         hreg_.removeHandler();
         hreg_ = null;
      }
   }

   private HandlerRegistration hreg_;
   private Display display_;
   private final Plots.Parent parent_;
   private String currentUrl_;
   private Size currentSize_;
   private final HandlerManager handlers_ = new HandlerManager(null);
}
