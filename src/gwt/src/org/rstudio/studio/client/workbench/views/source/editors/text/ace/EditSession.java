/*
 * EditSession.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class EditSession extends JavaScriptObject
{
   protected EditSession() {}

   public native final String getValue() /*-{
      return this.toString();
   }-*/;

   public native final void setValue(String code) /*-{
      this.setValue(code);
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
      var Mode = $wnd.require(parserName).Mode;
      this.setMode(new Mode(suppressHighlighting, this.getDocument(), this));
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

   public native final void removeMarker(int markerId) /*-{
      this.removeMarker(markerId);
   }-*/;
}
