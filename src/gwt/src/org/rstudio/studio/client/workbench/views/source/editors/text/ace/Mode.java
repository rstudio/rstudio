/*
 * Mode.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class Mode extends JavaScriptObject
{
   public static class InsertChunkInfo extends JavaScriptObject
   {
      protected InsertChunkInfo() {}

      public native final String getValue() /*-{
         return this.value;
      }-*/;

      public native final void setValue(String value) /*-{
         this.value = value;
      }-*/;

      /**
       * @return Position cursor should be navigated to, relative to the
       *       beginning of the value.
       */
      public native final Position getCursorPosition() /*-{
         return this.position || {row: 0, column: 0};
      }-*/;
      
      /**
       * @return Position of content, relative to the beginning of the value.
       */
      public native final Position getContentPosition() /*-{
         return this.content_position || {row: 0, column: 0};
      }-*/;
      
      
   }

   protected Mode()
   {
   }
   
   public native final boolean hasCodeModel() /*-{
      return typeof this.codeModel !== "undefined";
   }-*/;

   public native final CodeModel getCodeModel() /*-{
      return this.codeModel || {};
   }-*/;
   
   public native final CodeModel getRCodeModel() /*-{
      if (typeof this.r_codeModel !== "undefined")
         return this.r_codeModel;
      else
         return this.codeModel || {};
   }-*/;

   public native final String getLanguageMode(Position position) /*-{
      if (!this.getLanguageMode)
         return null;
      return this.getLanguageMode(position);
   }-*/;

   public native final FoldingRules getFoldingRules() /*-{
      return this.foldingRules;
   }-*/;

   public native final InsertChunkInfo getInsertChunkInfo() /*-{
      return this.insertChunkInfo || null;
   }-*/;
   
   public native final String getNextLineIndent(
         String state,
         String line,
         String tab,
         int tabSize,
         int row) /*-{
      return this.getNextLineIndent(state, line, tab, tabSize, row);
   }-*/;
   
   public native final Tokenizer getTokenizer() /*-{
      return this.$tokenizer;
   }-*/;
   
   public native final String getId() /*-{ return this.$id; }-*/;
   
   public native final void setHighlightRFunctionCalls(boolean show) /*-{
      this.setHighlightRFunctionCalls && this.setHighlightRFunctionCalls(show);
   }-*/;
   
   public native final void setInsertMatching(boolean value) /*-{
      this.insertMatching = value;
   }-*/;
   
}
