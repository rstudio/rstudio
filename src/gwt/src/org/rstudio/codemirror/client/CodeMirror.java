/*
 * CodeMirror.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.codemirror.client ;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.SpanElement;
import org.rstudio.core.client.dom.NativeWindow;

public class CodeMirror extends JavaScriptObject
{
   protected CodeMirror()
   {
   }
   
   public final native NativeWindow getWin() /*-{
      return this.win;
   }-*/;

   public final native DivElement getWrapping() /*-{
      return this.wrapping;
   }-*/;

   public final native IFrameElement getFrame() /*-{
      return this.frame;
   }-*/;
   
   public final void print() 
   {
      getWin().focus();
      getWin().print();
   }

   public static class SpanAndOffset extends JavaScriptObject
   {
      protected SpanAndOffset() {}

      public final native SpanElement getSpan() /*-{
         return this.span;
      }-*/;
      public final native int getOffset() /*-{
         return this.offset;
      }-*/;
   }

   public final native SpanAndOffset getSpanAtSelection(LineHandle line,
                                                      int characters,
                                                      boolean forward) /*-{

      function nextNode(node, allowChildren) {
         if (node == null)
            return null;

         var result = (allowChildren && node.firstChild) || node.nextSibling;
         if (result)
            return result;

         return nextNode(node.parentNode, false);
      }

      function findTextNode(line, offset) {
         var n = line;
         while ((n = nextNode(n, true)) != null) {
            if (n.nodeType == 3)
            {
               var len = n.nodeValue.length;
               if (offset < len || (!forward && offset == len))
                  return {node:n, offset:offset};
               offset -= len;
            }
         }
         return null;
      }

      function getParentSpan(node) {
         if (node == null)
            return null;
         while ((node = node.parentNode) != null) {
            if (node.nodeType == 1 && /^span$/i.test(node.nodeName))
               return node;
         }
         return null;
      }

      this.editor.highlightDirty(true);
      var nodeAndOffset = findTextNode(line || this.win.document.body,
                                       characters);
      if (nodeAndOffset == null)
         return null;
      var span = getParentSpan(nodeAndOffset.node);
      if (span == null)
         return null;
      return {span:span, offset:nodeAndOffset.offset};
   }-*/;

   public final native String getCode() /*-{
      return this.getCode();
   }-*/;

   public final native void setCode(String code) /*-{
      this.setCode(code);
   }-*/;

   public final native void setParserByName(String parser) /*-{
      this.setParser(parser);
   }-*/;

   public final native void setTextWrapping(boolean wrapping) /*-{
      this.setTextWrapping(wrapping);
   }-*/;

   public final native void focus() /*-{
      this.focus();
   }-*/;

   public final native String selection() /*-{
      return this.selection();
   }-*/;

   public final native void replaceSelection(String str) /*-{
      this.replaceSelection(str);
   }-*/;

   public final native CursorPosition cursorPosition(boolean start) /*-{
      return this.cursorPosition(start);
   }-*/;

   // firstLine() -> handle
   // lastLine() -> handle
   // nextLine(handle) -> handle
   // prevLine(handle) -> handle

   /**
    * NOTE: lineNum is 1-based, not zero.
    */
   public final native LineHandle nthLine(int lineNum) /*-{
      var result = this.nthLine(lineNum);
      if (!result)
      return null;
      return result;
   }-*/;

   public final native LineHandle firstLine() /*-{
      return this.firstLine();
   }-*/;

   public final native LineHandle lastLine() /*-{
      return this.lastLine();
   }-*/;

   public final native LineHandle nextLine(LineHandle lineHandle) /*-{
      return this.nextLine(lineHandle);

   }-*/;

   public final native LineHandle prevLine(LineHandle lineHandle) /*-{
      return this.prevLine(lineHandle);
   }-*/;

   public final native String lineContent(LineHandle handle) /*-{
      return this.lineContent(handle);
   }-*/;

   public final native void setLineContent(LineHandle handle, String strVal) /*-{
      this.setLineContent(handle, strVal);
   }-*/;

   public final native int lineNumber(LineHandle handle) /*-{
      return this.lineNumber(handle);
   }-*/;

   public final native void jumpToLine(LineHandle handle) /*-{
      this.jumpToLine(handle);
   }-*/;

   public final native void selectLines(LineHandle startHandle,
                                        int startOffset,
                                        LineHandle endHandle,
                                        int endOffset) /*-{
      // This first call ensures the beginning of the selection is made
      // visible (codemirror's normal behavior is to just make the end
      // of the selection visible, which can leave the entire selection
      // offscreen in some cases)
      this.selectLines(startHandle, startOffset);
   
      this.selectLines(startHandle, startOffset, endHandle, endOffset);
   }-*/;

   public final native void selectLine(LineHandle startHandle,
                                       int startOffset) /*-{
      this.selectLines(startHandle, startOffset);
   }-*/;


   public final native void insertIntoLine(LineHandle handle,
                                           int position,
                                           String text) /*-{
      this.insertIntoLine(handle, position, text);
   }-*/;

   public final native void setKeyDownHandler(CodeMirrorKeyHandler keyHandler) /*-{
      // monkeypatch the editor's keyDown
      this.editor.__keyDown = this.editor.keyDown;
      var thiz = this;
      this.editor.keyDown = function(event) {
         if (!keyHandler.@org.rstudio.codemirror.client.CodeMirrorKeyHandler::handleKey(Lcom/google/gwt/dom/client/NativeEvent;)(event))
            thiz.editor.__keyDown(event);
      };
   }-*/;

   public final native void setKeyPressHandler(CodeMirrorKeyHandler keyHandler) /*-{
      // monkeypatch the editor's keyDown
      this.editor.__keyPress = this.editor.keyPress;
      var thiz = this;
      this.editor.keyPress = function(event) {
         if (!keyHandler.@org.rstudio.codemirror.client.CodeMirrorKeyHandler::handleKey(Lcom/google/gwt/dom/client/NativeEvent;)(event))
            thiz.editor.__keyPress(event);
      };
   }-*/;

   public final native void removeScrollbars() /*-{
      this.win.document.body.style.overflow = "hidden";
   }-*/;
   public static class CursorPosition extends JavaScriptObject
   {

      protected CursorPosition()
      {
      }

      public static final native CursorPosition create(LineHandle line,
                                                       int character) /*-{
         return {line: line, character: character};
      }-*/;

      public final native LineHandle getLine() /*-{
         return this.line;
      }-*/;
      public final native int getCharacter() /*-{
         return this.character;
      }-*/;

   }
   public static class LineHandle extends JavaScriptObject
   {
      protected LineHandle()
      {
      }

   }
}
