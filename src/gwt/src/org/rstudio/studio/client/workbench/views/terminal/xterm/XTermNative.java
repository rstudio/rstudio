/*
 * XTermNative.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal.xterm;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

public class XTermNative extends JavaScriptObject
{
   protected XTermNative()
   {
   }
   
   public final native void open(Element container) /*-{
      this.open(container);
   }-*/;

   public final native void destroy() /*-{
      this.destroy();
   }-*/;
   
   public final native void fit() /*-{
      this.fit();
   }-*/;

   public final native void writeln(String data) /*-{
      this.writeln(data);
   }-*/;
   
   public final native void write(String data) /*-{
      this.write(data);
   }-*/;
  
   public final native void bell() /*-{
      this.bell();
   }-*/;
   
   public final native XTermDimensions proposeGeometry() /*-{
      return this.proposeGeometry();
   }-*/;
   
   public final native void focus() /*-{
      this.focus(); 
   }-*/; 
  
   public final native void scrollDisp(int lineCount) /*-{
      this.scrollDisp(lineCount, false);
   }-*/;
  
   public final native void scrollPages(int pageCount) /*-{
      this.scrollPages(pageCount);
   }-*/;
   
   public final native void scrollToTop() /*-{
      this.scrollToTop();
   }-*/;
   
   public final native void scrollToBottom() /*-{
      this.scrollToBottom();
   }-*/;
   
   public static native XTermNative createTerminal(boolean blink) /*-{
      return new $wnd.Terminal({cursorBlink: blink});
   }-*/;
} 