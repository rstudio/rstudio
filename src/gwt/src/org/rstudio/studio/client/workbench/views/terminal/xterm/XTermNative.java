/*
 * XTermNative.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import org.rstudio.core.client.CommandWithArg;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * <code>JavaScriptObject</code> wrapper for xterm.js
 * 
 * Reliance on xterm.js implementation details is marked with XTERM_IMP.
 * Be careful of these when updating to a newer xterm.js build.
 * The rest uses documented APIs: https://xtermjs.org/docs/api/terminal/
 */
public class XTermNative extends JavaScriptObject
{
   // Required by JavaScriptObject subclasses
   protected XTermNative()
   {
   }
   
   /**
    * Remove event handlers and detach from parent node.
    */
   public final native void destroy() /*-{
      this.destroy();
   }-*/;
   
   /**
    * Fit the terminal to available space.
    */
   public final native void fit() /*-{
      this.fit();
   }-*/;

   /**
    * Write a line of output to the terminal, appending CR/LF.
    * @param data String to write
    */
   public final native void writeln(String data) /*-{
      this.writeln(data);
   }-*/;
   
   /**
    * Write text to the terminal.
    * @param data String to write
    */
   public final native void write(String data) /*-{
      this.write(data);
   }-*/;
  
   /**
    * Compute and return available dimensions for terminal.
    * @return Visible number of columns and rows
    */
   public final native XTermDimensions proposeGeometry() /*-{
      return this.proposeGeometry();
   }-*/;
   
   public final native void focus() /*-{
      this.focus(); 
   }-*/; 
  
   public final native void blur() /*-{
      this.blur(); 
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

   public final native void reset() /*-{
      this.reset();
   }-*/;

   public final native void clear() /*-{
      this.clear();
   }-*/;

   public final native void addClass(String classStr) /*-{
      this.element.classList.add(classStr);
   }-*/;
   
   // XTERM_IMP
   public final native int cursorX() /*-{
      return this.buffer.x;
   }-*/;
   
   // XTERM_IMP
   public final native int cursorY() /*-{
      return this.buffer.y;
   }-*/;
 
   // XTERM_IMP
   public final native boolean altBufferActive() /*-{
      return this.buffers.active == this.buffers.alt;
   }-*/;
   
   public final native void showPrimaryBuffer() /*-{
      this.write("\x1b[?1047l"); // show primary buffer
      this.write("\x1b[m"); // reset all visual attributes
      this.write("\x1b[?9l"); // reset mouse mode
   }-*/;

   public final native void showAltBuffer() /*-{
      this.write("\x1b[?1047h"); // show alt buffer
   }-*/;
    
   // XTERM_IMP
   public final native String currentLine() /*-{
      lineBuf = this.buffer.lines.get(this.y + this.ybase);
      if (!lineBuf) // resize may be in progress
         return null;
      current = "";
      for (i = 0; i < this.cols; i++) {
         if (!lineBuf[i])
            return null;
         current += lineBuf[i][1];
      }
      return current;
   }-*/;

   // XTERM_IMP
   public final native String getLocalBuffer() /*-{
      buffer = "";
      for (row = 0; row < this.rows; row++) {
         lineBuf = this.buffer.lines.get(row);
         if (!lineBuf) // resize may be in progress
            return null;
      
         for (col = 0; col < this.cols; col++) {
            if (!lineBuf[col])
               return null;
            buffer += lineBuf[col][1];
         }
      }
      return buffer;
   }-*/;
    
   /**
    * Install a handler for user input (typing). Only one handler at a 
    * time may be installed. Previous handler will be overwritten.
    * @param command handler for data typed by the user
    */
   public final native void onTerminalData(CommandWithArg<String> command) /*-{
      this.handler = 
         $entry(function(data) {
            command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(data);
         });
   }-*/;

   /**
    * Install a handler for title events (via escape sequence).
    * @param command handler for title text
    */
   public final native void onTitleData(CommandWithArg<String> command) /*-{
      this.handleTitle = 
         $entry(function(title) {
            command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(title);
         });
   }-*/;

   /**
    * Factory to create a native Javascript terminal object.
    *  
    * @param container HTML element to attach to
    * @param blink <code>true</code> for a blinking cursor, otherwise solid cursor
    * @param focus <code>true</code> to give terminal focus by default
    * @param supportMousewheel <code>true</code> to handle legacy mousewheel event
    * 
    * @return Native Javascript Terminal object wrapped in a <code>JavaScriptObject</code>.
    */
   public static native XTermNative createTerminal(Element container, 
                                                   boolean blink,
                                                   boolean focus,
                                                   boolean supportMousewheel) /*-{
      var nativeTerm_ = new $wnd.Terminal({cursorBlink: blink});
      nativeTerm_.open(container, focus);

      // XTERM_IMP
      if (supportMousewheel) {
         // older browsers sent 'mousewheel' but xterm only handles 'wheel'
         // logic to translate from mousewheel event to wheel event taken from:
         // https://developer.mozilla.org/en-US/docs/Web/Events/wheel#Listening_to_this_event_across_browser
         self = nativeTerm_;
         nativeTerm_.element.addEventListener('mousewheel', function (ev) {
            if (self.mouseEvents)
               return;

            // create a normalized 'wheel' event object
            var event = {
               // keep a ref to the original event object
               ev: ev,
               target: ev.target || ev.srcElement,
               type: "wheel",
               deltaMode: ev.type == "MozMousePixelScroll" ? 0 : 1,
               deltaX: 0,
               deltaY: 0,
               deltaZ: 0,
               preventDefault: function() {
                  ev.preventDefault ?
                  ev.preventDefault() :
                  ev.returnValue = false;
               }
            };

            // calculate deltaY (and deltaX) according to the event
            event.deltaY = - 1/40 * ev.wheelDelta;

            // Webkit also support wheelDeltaX
            ev.wheelDeltaX && ( event.deltaX = - 1/40 * ev.wheelDeltaX );

            self.viewport.onWheel(event);
            return self.cancel(ev);
         });
      }
      return nativeTerm_;
   }-*/;
} 