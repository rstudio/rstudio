/*
 * XTermNative.java
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
   public final native void dispose() /*-{
      this.dispose();
   }-*/;

   /**
    * Fit the terminal to available space.
    */
   public final native void fit() /*-{
      this.rstudioFitAddon_.fit();
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
      return this.rstudioProposeDimensions();
   }-*/;

   public final native void focus() /*-{
      this.focus();
   }-*/;

   public final native void blur() /*-{
      this.blur();
   }-*/;

   public final native void scrollLines(int lineCount) /*-{
      this.scrollLines(lineCount);
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

   public final native void removeClass(String classStr) /*-{
      this.element.classList.remove(classStr);
   }-*/;

   // XTERM_IMP - using compatibility layer
   public final native int cursorX() /*-{
      return this.rstudioCursorX;
   }-*/;

   // XTERM_IMP - using compatibility layer
   public final native int cursorY() /*-{
      return this.rstudioCursorY;
   }-*/;

   // XTERM_IMP - using compatibility layer
   public final native boolean altBufferActive() /*-{
      return this.rstudioAltBufferActive();
   }-*/;

   public final native void showPrimaryBuffer() /*-{
      this.write("\x1b[?1047l"); // show primary buffer
      this.write("\x1b[m"); // reset all visual attributes
      this.write("\x1b[?9l"); // reset mouse mode
   }-*/;

   public final native void showAltBuffer() /*-{
      this.write("\x1b[?1047h"); // show alt buffer
   }-*/;

   // XTERM_IMP - using compatibility layer
   public final native String currentLine() /*-{
      return this.rstudioCurrentLine();
   }-*/;

   /**
    * Install a handler for user input (typing). Only one handler at a
    * time may be installed. Previous handler will be overwritten.
    * @param command handler for data typed by the user
    */
   public final native void onTerminalData(CommandWithArg<String> command) /*-{
      this.dataHandler = this.onData(
         $entry(function(data) {
            command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(data);
         }));
   }-*/;

   /**
    * Install a handler for title events (via escape sequence).
    * @param command handler for title text
    */
   public final native void onTitleData(CommandWithArg<String> command) /*-{
      this.handleTitle = this.onTitleChange(
         $entry(function(title) {
            command.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(title);
         }));
   }-*/;

   public final native void updateTheme(XTermTheme theme) /*-{
      this.rstudioSetOption("theme", theme);
   }-*/;

   public final native void updateBooleanOption(String option, boolean value) /*-{
      this.rstudioSetOption(option, value);
   }-*/;

   public final native void updateStringOption(String option, String value) /*-{
      this.rstudioSetOption(option, value);
   }-*/;

   public final native void updateDoubleOption(String option, double value) /*-{
      this.rstudioSetOption(option, value);
   }-*/;

   public final native void refresh() /*-{
      this.refresh(0, this.rows - 1);
   }-*/;

   public final native void setTabMovesFocus(boolean movesFocus) /*-{
      this.tabMovesFocus = movesFocus;
   }-*/;

   /**
    * Factory to create a native Javascript terminal object.
    *
    * @param container HTML element to attach to
    * @param options xterm.js settings
    *
    * @return Native Javascript Terminal object wrapped in a <code>JavaScriptObject</code>.
    */
   public static native XTermNative createTerminal(Element container,
                                                   XTermOptions options,
                                                   boolean clickableLinks,
                                                   boolean tabMovesFocus) /*-{
      try {
         var nativeTerm_ = new $wnd.Terminal(options);
         try {
            nativeTerm_.rstudioFitAddon_ = new $wnd.FitAddon.FitAddon();
            nativeTerm_.loadAddon(nativeTerm_.rstudioFitAddon_);
         } catch (error) {
            console.error("Error loading FitAddon: " + error);
         }
         if (clickableLinks) {
            try {
               nativeTerm_.rstudioLinksAddon_ = new $wnd.WebLinksAddon.WebLinksAddon();
               nativeTerm_.loadAddon(nativeTerm_.rstudioLinksAddon_);
            } catch (error) {
               console.error("Error loading WebLinksAddon: " + error);
            }
         }
         nativeTerm_.open(container);
         nativeTerm_.focus();
         nativeTerm_.tabMovesFocus = tabMovesFocus;

         nativeTerm_.attachCustomKeyEventHandler(function (event) {
            return !(event.keyCode === 9 && nativeTerm_.tabMovesFocus);
         });

         return nativeTerm_;
      } catch (error) {
         console.error('Error loading xterm.js: ' + error);
      }
      return undefined;
   }-*/;
}
