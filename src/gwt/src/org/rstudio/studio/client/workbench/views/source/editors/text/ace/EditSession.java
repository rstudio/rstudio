/*
 * EditSession.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.studio.client.workbench.views.output.lint.model.AceAnnotation;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class EditSession extends JavaScriptObject
{
   protected EditSession() {}

   public native final String getValue() /*-{
      return this.toString();
   }-*/;
   
   public native final String getState(int row) /*-{
      return this.getState(row);
   }-*/;
   
   public native final String getTabString() /*-{
      return this.getTabString();
   }-*/;
   
   public native final int getTabSize() /*-{
      return this.getTabSize();
   }-*/;

   public native final void setValue(String code) /*-{
      this.setValue(code);
   }-*/;
   
   public native final void setUseWorker(boolean useWorker) /*-{
      this.setUseWorker(useWorker);
   }-*/;

   public native final void insert(Position position, String text) /*-{
      this.insert(position, text);
   }-*/;

   public native final Selection getSelection() /*-{
      return this.getSelection();
   }-*/;
  
   public native final Position replace(Range range, String text) /*-{
      return this.replace(range, text);
   }-*/;

   public native final String getTextRange(Range range) /*-{
      return this.getTextRange(range);
   }-*/;

   public native final String getLine(int row) /*-{
      return this.getLine(row);
   }-*/;
   
   public native final JsArrayString getLines(int startRow, int endRow) /*-{
      return this.getLines(startRow, endRow);
   }-*/;

   public native final void setUseWrapMode(boolean useWrapMode) /*-{
      return this.setUseWrapMode(useWrapMode);
   }-*/;
   
   public native final void setWrapLimitRange(int min, int max) /*-{
      this.setWrapLimitRange(min, max);
   }-*/;

   public native final void setUseSoftTabs(boolean on) /*-{
      this.setUseSoftTabs(on);
   }-*/;

   public native final void setTabSize(int tabSize) /*-{
      this.setTabSize(tabSize);
   }-*/;

   /**
    * Number of rows
    */
   public native final int getLength() /*-{
      return this.getLength();
   }-*/;

   public native final void setEditorMode(String parserName,
                                          boolean suppressHighlighting) /*-{
      // find the appropriate editor mode and check to see whether it matches
      // the existing mode; if it does, no need to recreate it
      var Mode = $wnd.require(parserName).Mode;
      var existingMode = this.getMode();
      if (existingMode && existingMode.constructor == Mode)
         return;

      this.setMode(new Mode(suppressHighlighting, this));
   }-*/;

   public native final Mode getMode() /*-{
      return this.getMode();
   }-*/;

   public native final void setDisableOverwrite(boolean disableOverwrite) /*-{
      this.setDisableOverwrite(disableOverwrite);
   }-*/;

   public native final int documentToScreenRow(Position position) /*-{
      return this.documentToScreenRow(position.row, position.column);
   }-*/;

   public native final int getScreenLength() /*-{
      return this.getScreenLength();
   }-*/;
   
   public native final void setScrollLeft(int left) /*-{
      this.setScrollLeft(left);
   }-*/;
   
   public native final void setScrollTop(int top) /*-{
      this.setScrollTop(top);
   }-*/;

   public native final int getScrollTop() /*-{
      return this.getScrollTop();
   }-*/;

   public native final UndoManager getUndoManager() /*-{
      return this.getUndoManager();
   }-*/;

   public native final Document getDocument() /*-{
      return this.getDocument();
   }-*/;

   public native final void setNewLineMode(String type) /*-{
      this.setNewLineMode(type);
   }-*/;

   public native final void reindent(Range range) /*-{
      this.reindent(range);
   }-*/;

   public native final void foldAll() /*-{
      this.foldAll();
   }-*/;

   public native final void unfoldAll() /*-{
      this.unfold();
   }-*/;

   public native final void toggleFold() /*-{
      this.toggleFold(false);
   }-*/;

   public native final JsArray<AceFold> getAllFolds() /*-{
      return this.getAllFolds();
   }-*/;
   
   public native final AceFold getFoldAt(Position position) /*-{
      return this.getFoldAt(position.row, position.column);
   }-*/;

   public native final AceFold getFoldAt(int row, int column) /*-{
      return this.getFoldAt(row, column);
   }-*/;
   
   public native final void addFold(String placeholder, Range range) /*-{
      this.addFold(placeholder, range);
   }-*/;

   public native final void unfold(Range range, boolean expandInner) /*-{
      this.unfold(range, expandInner);
   }-*/;

   public native final void unfold(Position pos, boolean expandInner) /*-{
      this.unfold(pos, expandInner);
   }-*/;

   public native final void unfold(int row, boolean expandInner) /*-{
      this.unfold(row, expandInner);
   }-*/;

   public native final int addMarker(Range range,
                                     String clazz,
                                     String type,
                                     boolean inFront) /*-{
      return this.addMarker(range, clazz, type, inFront);
   }-*/;
   
   public native final int addMarker(Range range,
                                     String clazz,
                                     JavaScriptObject renderer,
                                     boolean inFront)
   /*-{
      return this.addMarker(range, clazz, renderer, inFront);
   }-*/;

   public native final void removeMarker(int markerId) /*-{
      this.removeMarker(markerId);
   }-*/;
   
   public native final void setBreakpoint(int line) /*-{
      this.setBreakpoint(line);
   }-*/;

   public native final void clearBreakpoint(int line) /*-{
      this.clearBreakpoint(line);
   }-*/;
   
   public native final void setBreakpoints(int[] lines) /*-{
      this.setBreakpoints(lines);
   }-*/;
   
   public native final void clearBreakpoints(int[] lines) /*-{
      this.clearBreakpoints(lines);
   }-*/;
   
   public native final void setAnnotations(JsArray<AceAnnotation> annotations) /*-{
      this.setAnnotations(annotations);
   }-*/;
   
   public native final JsArray<AceAnnotation> getAnnotations() /*-{
      return this.getAnnotations();
   }-*/;
   
   public native final Markers getMarkers(boolean inFront) /*-{
      return this.getMarkers(inFront);
   }-*/;
   
   public native final Marker getMarker(int id) /*-{
      return this.getMarkers(true)[id];
   }-*/;
   
   public final AnchoredRange createAnchoredRange(Position start, Position end)
   {
      return createAnchoredRange(start, end, true);
   }
   
   public final native AnchoredRange createAnchoredRange(Position start,
                                                         Position end,
                                                         boolean insertRight)
   /*-{
      var Range = $wnd.require("ace/range").Range;
      var result = new Range();
      result.start = this.doc.createAnchor(start.row, start.column);
      result.end = this.doc.createAnchor(end.row, end.column);
      result.end.$insertRight = insertRight;
      return result;
   }-*/;
   
   public native final void setWorkerTimeout(int delayMs) /*-{
      var worker = this.$worker;
      if (worker && worker.setTimeout)
         worker.setTimeout(delayMs);
   }-*/;
   
   public final Token getTokenAt(Position position)
   {
      return getTokenAt(position.getRow(), position.getColumn());
   }
   
   public native final JsArray<Token> getTokens(int row) /*-{
      return this.getTokens(row);
   }-*/;
   
   public native final Token getTokenAt(int row, int column) /*-{
      var token = this.getTokenAt(row, column);
      if (token == null)
         return null;
      token.column = token.start;
      return token;
   }-*/;
   
   public native final void setFoldStyle(String style) /*-{
      this.setFoldStyle(style);
   }-*/;

}
