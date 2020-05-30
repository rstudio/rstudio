/*
 * JavaScriptEventHistory.java
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
package org.rstudio.studio.client;

import java.util.LinkedList;
import java.util.Queue;

import org.rstudio.core.client.widget.MiniPopupPanel;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Singleton;

// This class maintains a simple JavaScript event history, primarily to be used
// by classes that need to know what sequence of unfortunate events might have
// led to the current application state (whatever that might be).

@Singleton
public class JavaScriptEventHistory
{
   public static class EventData extends JavaScriptObject
   {
      protected EventData()
      {
      }
      
      public static final native EventData create(NativeEvent event)
      /*-{
         return {
            "type"   : event.type   || "",
            "button" : event.button || -1
         };
      }-*/;
      
      public final native String getType() /*-{ return this["type"];   }-*/;
      public final native int getButton()  /*-{ return this["button"]; }-*/;
      
      public static final int BUTTON_NONE   = -1;
      public static final int BUTTON_LEFT   =  0;
      public static final int BUTTON_MIDDLE =  1;
      public static final int BUTTON_RIGHT  =  2;
   }
   
   public interface Predicate
   {
      public boolean accept(EventData event);
   }
   
   public JavaScriptEventHistory()
   {
      queue_ = new LinkedList<EventData>();
      registerEventListeners();
   }
   
   private final native void registerEventListeners()
   /*-{
      var self = this;
      
      // get reference to document body
      var document = $doc;
      var body = document.body;
      
      // define our handler (we can just use a single one)
      var handler = $entry(function(event) {
         self.@org.rstudio.studio.client.JavaScriptEventHistory::onEvent(Lcom/google/gwt/dom/client/NativeEvent;)(event);
      });
      
      // define the events that we want to listen to
      var events = [
         "cut", "copy", "paste",
         "resize", "scroll",
         "keydown", "keypress", "keyup",
         "mouseenter", "mouseleave", "mouseover", "mousemove", "mouseout",
         "mousedown", "mouseup", "click", "dblclick", "contextmenu", "wheel",
         "drag", "dragstart", "dragend", "dragenter", "dragover", "dragleave", "drop"
      ];
         
      
      // iterate through all keys on body object, and
      // register handlers for any prefixed with 'on'
      for (var i = 0, n = events.length; i < n; i++) {
         body.addEventListener(events[i], handler, true)
      }
      
   }-*/;
   
   private void onEvent(NativeEvent event)
   {
      EventData eventData = EventData.create(event);
      queue_.add(eventData);
      if (queue_.size() > QUEUE_LENGTH)
         queue_.remove();
   }
   
   public EventData findEvent(Predicate predicate)
   {
      for (EventData data : queue_)
         if (predicate.accept(data))
            return data;
      return null;
   }
   
   // primarily for debug use (see what events are being emitted over time)
   private MiniPopupPanel historyPanel_ = null;
   public void debugHistory()
   {
      if (historyPanel_ == null)
         historyPanel_ = new MiniPopupPanel(false, false);
      
      VerticalPanel contentPanel = new VerticalPanel();
      contentPanel.add(new HTML("<h4 style='margin: 0;'>JavaScript Event History</h2><hr />"));
      
      int i = 0, n = Math.min(10, queue_.size());
      for (EventData event : queue_)
      {
         if (i++ == n)
            break;
         contentPanel.add(new HTML("Event: " + event.getType()));
      }
      
      historyPanel_.setWidget(contentPanel);
      if (!historyPanel_.isShowing())
      {
         historyPanel_.setPopupPosition(10, 10);
         historyPanel_.show();
      }
   }
   
   private final Queue<EventData> queue_;
   private static final int QUEUE_LENGTH = 10;
}
