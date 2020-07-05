/*
 * XTermTerminal.java
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
package org.rstudio.studio.client.workbench.views.terminal.xterm;

import com.google.gwt.dom.client.Element;
import elemental2.core.Uint8Array;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLTextAreaElement;
import elemental2.dom.KeyboardEvent;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;
import org.rstudio.core.client.jsinterop.JsConsumerWithArg;
import org.rstudio.core.client.jsinterop.JsIntConsumer;
import org.rstudio.core.client.jsinterop.JsVoidFunction;

/**
 * The class that represents an xterm.js terminal (Terminal).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Terminal")
public class XTermTerminal extends XTermDisposable
{
   /**
    * The element containing the terminal.
    */
   @JsProperty public native HTMLElement getElement();

   /**
    * The textarea that accepts input for the terminal.
    */
   @JsProperty public native HTMLTextAreaElement getTextarea();

   /**
    * The number of rows in the terminal's viewport. Use
    * `ITerminalOptions.rows` to set this in the constructor and
    * `Terminal.resize` for when the terminal exists.
    */
   @JsProperty public native int getRows();

   /**
    * The number of columns in the terminal's viewport. Use
    * `ITerminalOptions.cols` to set this in the constructor and
    * `Terminal.resize` for when the terminal exists.
    */
   @JsProperty public native int getCols();

   /**
    * (EXPERIMENTAL) The terminal's current buffer, this might be either the
    * normal buffer or the alt buffer depending on what's running in the
    * terminal.
    */
   @JsProperty public native XTermBufferNamespace getBuffer();

   /**
    * (EXPERIMENTAL) Get all markers registered against the buffer. If the alt
    * buffer is active this will always return [].
    */
   @JsProperty public native XTermMarker[] getMarkers();

   /**
    * (EXPERIMENTAL) Get the parser interface to register
    * custom escape sequence handlers.
    */
   public XTermParser parser;

   /**
    * (EXPERIMENTAL) Get the Unicode handling interface
    * to register and switch Unicode version.
    */
   public XTermUnicodeHandling unicode;

   /**
    * Natural language strings that can be localized.
    */
   public static XTermLocalizableStrings strings;

   /**
    * Creates a new `Terminal` object.
    *
    * @param options An object containing a set of options.
    */
   public XTermTerminal(XTermOptions options) {}

   /**
    * Adds an event listener for when a binary event fires. This is used to
    * enable non UTF-8 conformant binary messages to be sent to the backend.
    * Currently this is only used for a certain type of mouse reports that
    * happen to be not UTF-8 compatible.
    * The event value is a JS string, pass it to the underlying pty as
    * binary data, e.g. `pty.write(Buffer.from(data, 'binary'))`.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onBinary(JsConsumerWithArg<String> callback);

   /**
    * Adds an event listener for the cursor moves.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onCursorMove(JsVoidFunction callback);


   /**
    * Adds an event listener for when a data event fires. This happens for
    * example when the user types or pastes into the terminal. The event value
    * is whatever `string` results, in a typical setup, this should be passed
    * on to the backing pty.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onData(JsConsumerWithArg<String> callback);

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class OnKeyCallback
   {
      String key;
      KeyboardEvent domEvent;
   }

   /**
    * Adds an event listener for when a key is pressed. The event value contains the
    * string that will be sent in the data event as well as the DOM event that
    * triggered it.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onKey(JsConsumerWithArg<OnKeyCallback> callback);

   /**
    * Adds an event listener for when a line feed is added.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onLineFeed(JsVoidFunction callback);

   /**
    * Adds an event listener for when a scroll occurs. The event value is the
    * new position of the viewport.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onScroll(JsIntConsumer callback);

   /**
    * Adds an event listener for when a selection change occurs.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onSelectionChange(JsVoidFunction callback);

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class OnRenderCallback
   {
      int start;
      int end;
   }

   /**
    * Adds an event listener for when rows are rendered. The event value
    * contains the start row and end rows of the rendered area (ranges from `0`
    * to `Terminal.rows - 1`).
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onRender(OnRenderCallback callback);

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class OnResizeCallback
   {
      int cols;
      int rows;
   }

   /**
    * Adds an event listener for when the terminal is resized. The event value
    * contains the new size.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onResize(OnResizeCallback callback);

   /**
    * Adds an event listener for when an OSC 0 or OSC 2 title change occurs.
    * The event value is the new title.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onTitleChange(JsConsumerWithArg<String> callback);

   /**
    * Unfocus the terminal.
    */
   public native void blur();

   /**
    * Focus the terminal.
    */
   public native void focus();

   /**
    * Resizes the terminal. It's best practice to debounce calls to resize,
    * this will help ensure that the pty can respond to the resize event
    * before another one occurs.
    *
    * @param x The number of columns to resize to.
    * @param y The number of rows to resize to.
    */
   public native void resize(int x, int y);

   /**
    * Opens the terminal within an element.
    *
    * @param parent The element to create the terminal within. This element
    *               must be visible (have dimensions) when `open` is called as several DOM-
    *               based measurements need to be performed when this function is called.
    */
   public native void open(HTMLElement parent);

   @JsOverlay public final void open(Element parent)
   {
      HTMLElement element = Js.uncheckedCast(parent);
      open(element);
   }

   @JsFunction public interface KeyEventHandler
   {
      boolean handleKeyEvent(elemental2.dom.KeyboardEvent event);
   }

   /**
    * Attaches a custom key event handler which is run before keys are
    * processed, giving consumers of xterm.js ultimate control as to what keys
    * should be processed by the terminal and what keys should not.
    * @param customKeyEventHandler The custom KeyboardEvent handler to attach.
    * This is a function that takes a KeyboardEvent, allowing consumers to stop
    * propagation and/or prevent the default action. The function returns
    * whether the event should be processed by xterm.js.
    */
   public native void attachCustomKeyEventHandler(KeyEventHandler customKeyEventHandler);

   /**
    * (EXPERIMENTAL) Registers a link provider, allowing a custom parser to
    * be used to match and handle links. Multiple link providers can be used,
    * they will be asked in the order in which they are registered.
    * @param linkProvider The link provider to use to detect links.
    */
   public native XTermDisposable registerLinkProvider(XTermLinkProvider linkProvider);

   /**
    * (EXPERIMENTAL) Registers a character joiner, allowing custom sequences of
    * characters to be rendered as a single unit. This is useful in particular
    * for rendering ligatures and graphemes, among other things.
    *
    * Each registered character joiner is called with a string of text
    * representing a portion of a line in the terminal that can be rendered as
    * a single unit. The joiner must return a sorted array, where each entry is
    * itself an array of length two, containing the start (inclusive) and end
    * (exclusive) index of a substring of the input that should be rendered as
    * a single unit. When multiple joiners are provided, the results of each
    * are collected. If there are any overlapping substrings between them, they
    * are combined into one larger unit that is drawn together.
    *
    * All character joiners that are registered get called every time a line is
    * rendered in the terminal, so it is essential for the handler function to
    * run as quickly as possible to avoid slowdowns when rendering. Similarly,
    * joiners should strive to return the smallest possible substrings to
    * render together, since they aren't drawn as optimally as individual
    * characters.
    *
    * NOTE: character joiners are only used by the canvas renderer.
    *
    * @param handler The function that determines character joins. It is called
    * with a string of text that is eligible for joining and returns an array
    * where each entry is an array containing the start (inclusive) and end
    * (exclusive) indexes of ranges that should be rendered as a single unit.
    * @return The ID of the new joiner, this can be used to deregister
    */
   // Not currently implemented
   // registerCharacterJoiner(handler: (text: string) => [number, number][]): number;

   /**
    * (EXPERIMENTAL) Deregisters the character joiner if one was registered.
    * NOTE: character joiners are only used by the canvas renderer.
    * @param joinerId The character joiner's ID (returned after register)
    */
   // Not currently implemented
   // deregisterCharacterJoiner(joinerId: number): void;

   /**
    * (EXPERIMENTAL) Adds a marker to the normal buffer and returns it. If the
    * alt buffer is active, undefined is returned.
    * @param cursorYOffset The y position offset of the marker from the cursor.
    */
   public native XTermMarker registerMarker(int cursorYOffset);

   /**
    * Gets whether the terminal has an active selection.
    */
   public native boolean hasSelection();

   /**
    * Gets the terminal's current selection, this is useful for implementing
    * copy behavior outside of xterm.js.
    */
   public native String getSelection();

   /**
    * Gets the selection position or undefined if there is no selection.
    */
   public native XTermSelectionPosition getSelectionPosition();

   /**
    * Clears the current terminal selection.
    */
   public native void clearSelection();

   /**
    * Selects text within the terminal.
    *
    * @param column The column the selection starts at.
    * @param row    The row the selection starts at.
    * @param length The length of the selection.
    */
   public native void select(int column, int row, int length);

   /**
    * Selects all text within the terminal.
    */
   public native void selectAll();

   /**
    * Selects text in the buffer between 2 lines.
    *
    * @param start The 0-based line index to select from (inclusive).
    * @param end   The 0-based line index to select to (inclusive).
    */
   public native void selectLines(int start, int end);

   /**
    * Scroll the display of the terminal
    *
    * @param amount The number of lines to scroll down (negative scroll up).
    */
   public native void scrollLines(int amount);

   /**
    * Scroll the display of the terminal by a number of pages.
    *
    * @param pageCount The number of pages to scroll (negative scrolls up).
    */
   public native void scrollPages(int pageCount);

   /**
    * Scrolls the display of the terminal to the top.
    */
   public native void scrollToTop();

   /**
    * Scrolls the display of the terminal to the bottom.
    */
   public native void scrollToBottom();

   /**
    * Scrolls to a line within the buffer.
    *
    * @param line The 0-based line index to scroll to.
    */
   public native void scrollToLine(int line);

   /**
    * Clear the entire buffer, making the prompt line the new first line.
    */
   public native void clear();

   /**
    * Write data to the terminal.
    *
    * @param data     The data to write to the terminal. This can either be raw
    *                 bytes given as Uint8Array from the pty or a string. Raw bytes will always
    *                 be treated as UTF-8 encoded, string data as UTF-16.
    * @param callback Optional callback that fires when the data was processed
    *                 by the parser.
    */
   public native void write(String data, JsVoidFunction callback);
   public native void write(Uint8Array data, JsVoidFunction callback);

   /**
    * Writes data to the terminal, followed by a break line character (\n).
    *
    * @param data     The data to write to the terminal. This can either be raw
    *                 bytes given as Uint8Array from the pty or a string. Raw bytes will always
    *                 be treated as UTF-8 encoded, string data as UTF-16.
    * @param callback Optional callback that fires when the data was processed
    *                 by the parser.
    */
   public native void writeln(String data, JsVoidFunction callback);
   public native void writeln(Uint8Array data, JsVoidFunction callback);

   /**
    * Writes text to the terminal, performing the necessary transformations for pasted text.
    *
    * @param data The text to write to the terminal.
    */
   public native void paste(String data);

   /**
    * Retrieves an option's value from the terminal.
    *
    * @param key The option key.
    */
   public native Any getOption(String key);

   /**
    * Sets an option on the terminal.
    *
    * @param key   The option key.
    * @param value The option value.
    */
   public native void setOption(String key, Any value);

   /**
    * Tells the renderer to refresh terminal content between two rows
    * (inclusive) at the next opportunity.
    *
    * @param start The row to start from (between 0 and this.rows - 1).
    * @param end   The row to end at (between start and this.rows - 1).
    */
   public native void refresh(int start, int end);

   /**
    * Perform a full reset (RIS, aka '\x1bc').
    */
   public native void reset();

   /**
    * Loads an addon into this instance of xterm.js.
    *
    * @param addon The addon to load.
    */
   public native void loadAddon(XTermAddon addon);

   /**
    * Add a class to the element containing the terminal.
    *
    * @param classStr Class to add
    */
   @JsOverlay public final void addClass(String classStr)
   {
      getElement().classList.add(classStr);
   }

   /**
    * Remove a class from the element containing the terminal
    * @param classStr Class to remove
    */
   @JsOverlay public final void removeClass(String classStr)
   {
      getElement().classList.remove(classStr);
   }
}

