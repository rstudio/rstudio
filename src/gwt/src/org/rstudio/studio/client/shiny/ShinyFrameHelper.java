/*
 * ShinyFrameHelper.java
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
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.Operation;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class ShinyFrameHelper
{
   private static class ShinyFrameEvent extends JavaScriptObject
   {
      protected ShinyFrameEvent() {}
      public final native String getEvent() /*-{
         return this.data.event;
      }-*/;
      public final native int getIntData() /*-{
         return this.data.data;
      }-*/;
      public final native String getStringData() /*-{
         return this.data.data;
      }-*/;
      public final native String getOrigin() /*-{
         return this.origin;
      }-*/;
      public final native WindowEx getSource() /*-{
         return this.source;
      }-*/;
   }
   
   private static class ShinyFrameMethod extends JavaScriptObject
   {
      protected ShinyFrameMethod() {}
      public final native static ShinyFrameMethod create(String method, 
                                                         int arg) /*-{
         return { method: method, arg: arg };
      }-*/;
      public final native static ShinyFrameMethod create(String method, 
                                                         String arg) /*-{
         return { method: method, arg: arg };
      }-*/;
      public final native String getMethod() /*-{
         return this.method;
      }-*/;
   }
   
   public void initialize(String url, Operation onComplete)
   {
      // remember the URL and begin waiting for the window object to arrive
      url_ = url;
      window_ = null;
      origin_ = null;
      onInitComplete_ = onComplete;
   }

   public ShinyFrameHelper()
   {
      initializeEvents();
   }
   
   public int getScrollPosition() 
   {
      return scrollPosition_;
   }
   
   public String getUrl()
   {
      return url_;
   }
   
   public void setScrollPosition(int pos)
   {
      sendMethod(ShinyFrameMethod.create(METHOD_SET_SCROLL, pos));
   }
   
   public void setHash(String hash)
   {
      sendMethod(ShinyFrameMethod.create(METHOD_SET_HASH, hash));
   }
   
   // Private methods ---------------------------------------------------------

   private native void initializeEvents() /*-{  
      var thiz = this;   
      $wnd.addEventListener(
            "message",
            $entry(function(e) {
               if (typeof e.data != 'object')
                  return;
               if (e.data.type !== 'ShinyFrameEvent')
                  return;
               thiz.@org.rstudio.studio.client.shiny.ShinyFrameHelper::onMessage(Lcom/google/gwt/core/client/JavaScriptObject;)(e);
            }),
            true);
   }-*/;
   
   private void onMessage(JavaScriptObject data)
   {  
      // reject messages that don't match the expected origin
      ShinyFrameEvent event = data.cast();
      if (!url_.startsWith(event.getOrigin())) 
         return;

      String eventName = event.getEvent();
      if (eventName == EVENT_SCROLL_CHANGE)
      {
         scrollPosition_ = event.getIntData();
      }
      else if (eventName == EVENT_HASH_CHANGE)
      {
         url_ = event.getStringData();
      }
      else if (eventName == EVENT_READY)
      {
         window_ = event.getSource();
         origin_ = event.getOrigin();
         if (onInitComplete_ != null)
         {
            onInitComplete_.execute();
            onInitComplete_ = null;
         }
      }
   }
   
   private void sendMethod(final ShinyFrameMethod method)
   {
      // if we don't have a valid window object yet, try again until we do
      // (every 100ms, up to 5s)
      if (window_ == null)
      {
         windowRetries_ = 0;
         Scheduler.get().scheduleFixedPeriod(new RepeatingCommand()
         {
            @Override
            public boolean execute()
            {
               if (window_ == null && windowRetries_ < MAX_WINDOW_RETRIES)
               {
                  windowRetries_++;
                  return true;
               }
               if (window_ != null)
               {
                  window_.postMessage(method, origin_);
               }
               return false;
            }
         }, WINDOW_RETRY_DELAY);
      }
      else 
      {
         window_.postMessage(method, origin_);
      }
   }
   
   public WindowEx getWindow()
   {
      return window_;
   }

   private WindowEx window_ = null;

   private int scrollPosition_ = 0;
   private String url_ = "";
   private String origin_ = "";
   private Operation onInitComplete_;
   
   // how many times and how long we're willing to wait for a window object to
   // appear to communicate with
   private int windowRetries_ = 0;
   private final static int MAX_WINDOW_RETRIES = 50;
   private final static int WINDOW_RETRY_DELAY = 100;
         
   // event names (must match those in rsiframe.js)
   private final static String EVENT_SCROLL_CHANGE = "doc_scroll_change";
   private final static String EVENT_HASH_CHANGE = "doc_hash_change";
   private final static String EVENT_READY = "doc_ready";
   private final static String METHOD_SET_SCROLL = "rs_set_scroll_pos";
   private final static String METHOD_SET_HASH = "rs_set_hash";
}
