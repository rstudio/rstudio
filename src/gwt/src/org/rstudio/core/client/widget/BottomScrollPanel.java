/*
 * BottomScrollPanel.java
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

import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;

/**
 * An implementation of ScrollPanel that defaults its scroll position to the
 * bottom. If resized while the scroll position is at the bottom, the scroll
 * position will stay at the bottom (even if the panel is shrinking in size).
 */
public class BottomScrollPanel extends ScrollPanel
{
   public BottomScrollPanel()
   {
      scrolling_ = false;
      
      // use a timer to help debounce scroll events
      scrollTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            scrollToBottom();
         }
      };
      
      // Provide a close-enough zone for determining if scrolled
      // to the bottom; allows for small rounding errors that have
      // been seen on zoomed high-DPI displays.
      final int vFudge = 4;
      addScrollHandler(new ScrollHandler()
      {
         public void onScroll(ScrollEvent event)
         {
            // short circuit if we initiated this scroll
            if (scrolling_)
               return;
            
            scrolledToBottom_ = 
             
             (Math.abs(getVerticalScrollPosition() - 
                       getMaximumVerticalScrollPosition()) <= vFudge) || 
              
             (Math.abs((getVerticalScrollPosition() + getOffsetHeight()) - 
                        getElement().getScrollHeight()) <= vFudge);
         }
      });
   }

   public BottomScrollPanel(Widget widget)
   {
      this();
      setWidget(widget);
   }

   public boolean isScrolledToBottom()
   {
      return scrolledToBottom_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      scrollToBottom();
   }

   @Override
   public void onResize()
   {
      if (scrolledToBottom_)
         scrollToBottom();
      super.onResize();
   }

   @Override
   public void scrollToBottom()
   {
      scrolling_ = true;
      try
      {
         DomUtils.scrollToBottom(getElement());
      }
      catch(Exception e)
      {
         Debug.logException(e);
      }
      scrolling_ = false;
      scrolledToBottom_ = true;
   }

   public void onContentSizeChanged()
   {
      // keep the user viewport at the bottom if they've scrolled to the bottom
      if (!scrolledToBottom_)
         return;
      
      // allow an existing timer to continue running (ensures
      // that we attempt a scroll every few ticks as appropriate)
      if (scrollTimer_.isRunning())
         return;

      // schedule a new scroll if we haven't done this yet
      scrollTimer_.schedule(SCROLL_DELAY_MS);
   }

   public void saveScrollPosition()
   {
      vScroll_ = scrolledToBottom_ ? null : getVerticalScrollPosition();
      hScroll_ = getHorizontalScrollPosition();
   }

   public void restoreScrollPosition()
   {
      if (vScroll_ == null)
         scrollToBottom();
      else
         setVerticalScrollPosition(vScroll_);

      setHorizontalScrollPosition(hScroll_);
   }
   
   public boolean hasSavedScrollPosition()
   {
      return vScroll_ != null;
   }
   
   private final Timer scrollTimer_;
   private static final int SCROLL_DELAY_MS = 50;
   
   private boolean scrolledToBottom_;
   private boolean scrolling_;
   private Integer vScroll_;
   private int hScroll_;
}
