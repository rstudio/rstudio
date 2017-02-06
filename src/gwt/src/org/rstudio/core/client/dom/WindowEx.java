/*
 * WindowEx.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import org.rstudio.core.client.Point;

public class WindowEx extends JavaScriptObject
{
   public static native WindowEx get() /*-{
      return $wnd;
   }-*/;

   protected WindowEx()
   {
   }

   public final native void focus() /*-{
      this.focus();
   }-*/;

   public final native void print() /*-{
      this.print() ;
   }-*/;

   public final native void back() /*-{
      this.history.back() ;
   }-*/;
   
   public final native void forward() /*-{
      this.history.forward() ;
   }-*/;
   
   public final native void removeSelection() /*-{
      selection = this.getSelection();
      if (selection != null) {
         selection.removeAllRanges();
      }
   }-*/;
   
   public final native String getSelectedText() /*-{
      return this.getSelection().toString();
   }-*/;
   
   public final native boolean find(String term, 
                                    boolean matchCase,
                                    boolean searchUpward,
                                    boolean wrapAround,
                                    boolean wholeWord) /*-{
      return this.find(term, matchCase, searchUpward, wrapAround, wholeWord);
   }-*/;
   
   public final native String getLocationHref() /*-{
      return this.location.href ;
   }-*/;
   
   public final native boolean isSecure() /*-{
      return 'https:' == this.location.protocol;
   }-*/;

   public final native void reload() /*-{
      this.location.reload(true);
   }-*/;
  
   public final native void setLocationHref(String helpURL) /*-{
      this.location.href = helpURL ;
   }-*/;

   public final native void replaceLocationHref(String helpURL) /*-{
      this.location.replace(helpURL) ;
   }-*/;
   
   public final native void replaceHistoryState(String url) /*-{
      this.history.replaceState({}, "", url);
   }-*/;

   public final Point getScrollPosition()
   {
      JsArrayInteger pos = getScrollPositionInternal();
      return new Point(pos.get(0), pos.get(1));
   }

   public final void setScrollPosition(Point pos)
   {
      setScrollPositionInternal(pos.x, pos.y);
   }

   private final native JsArrayInteger getScrollPositionInternal() /*-{
      return [this.scrollX, this.scrollY];
   }-*/;

   private final native void setScrollPositionInternal(int x, int y) /*-{
      this.scrollTo(x, y);
   }-*/;

   public final native void close() /*-{
      this.close();
   }-*/;
   
   public final native boolean isClosed() /*-{
      // On the desktop, it is possible in some circumstances for satellite
      // window objects to become decoupled from their physical windows when
      // closed--they are still marked open but are effectively zombies. To work
      // around this we have the desktop frame manually label the window object
      // as closed with rstudioSatelliteClosed so that we can appropriately
      // treat it as closed.
      return this.closed || this.rstudioSatelliteClosed;
   }-*/;

   public final native void resizeTo(int width, int height) /*-{
      this.resizeTo(width, height);
   }-*/;

   public final native DocumentEx getDocument() /*-{
      return this.document;
   }-*/;
   
   public final native int getLeft() /*-{
      return this.screenX;
   }-*/;

   public final native int getTop() /*-{
      return this.screenY;
   }-*/;
   
   public final native int getOuterHeight() /*-{
      return this.outerHeight;
   }-*/;

   public final native int getOuterWidth() /*-{
      return this.outerWidth;
   }-*/;
   
   public final native int getInnerHeight() /*-{
      return this.innerHeight;
   }-*/;

   public final native int getInnerWidth() /*-{
      return this.innerWidth;
   }-*/;
   
   public final native void scrollTo(int x, int y) /*-{
      this.scrollTo(x, y);
   }-*/;

   public final native int getScrollLeft() /*-{
      return this.scrollX;
   }-*/;

   public final native int getScrollTop() /*-{
      return this.scrollY;
   }-*/;
   
   public final native int getScreenX() /*-{
      return this.screenX;
   }-*/;
   
   public final native int getScreenY() /*-{
      return this.screenY;
   }-*/;

   public final native void postMessage(JavaScriptObject data, 
                                        String origin) /*-{
      this.postMessage(data, origin);
   }-*/;

   public static HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return handlers_.addHandler(FocusEvent.getType(), handler);
   }

   public static HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return handlers_.addHandler(BlurEvent.getType(), handler);
   }

   private static void fireFocusHandlers()
   {
      NativeEvent nativeEvent = Document.get().createFocusEvent();
      FocusEvent.fireNativeEvent(nativeEvent, new HasHandlers()
      {
         public void fireEvent(GwtEvent<?> event)
         {
            handlers_.fireEvent(event);
         }
      });
   }

   private static void fireBlurHandlers()
   {
      NativeEvent nativeEvent = Document.get().createBlurEvent();
      BlurEvent.fireNativeEvent(nativeEvent, new HasHandlers()
      {
         public void fireEvent(GwtEvent<?> event)
         {
            handlers_.fireEvent(event);
         }
      });
   }
   
   static {
      registerNativeListeners();
   }

   private static native void registerNativeListeners() /*-{
      $wnd.onfocus = function() {
         @org.rstudio.core.client.dom.WindowEx::fireFocusHandlers()();
      };
      $wnd.onblur = function() {
         @org.rstudio.core.client.dom.WindowEx::fireBlurHandlers()();
      };
   }-*/;

   private static final HandlerManager handlers_ = new HandlerManager(null);
}
