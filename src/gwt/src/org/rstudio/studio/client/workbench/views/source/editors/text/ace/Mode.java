/*
 * Mode.java
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

public class Mode extends JavaScriptObject
{
   public static class InsertChunkInfo extends JavaScriptObject
   {
      protected InsertChunkInfo() {}

      public native final String getValue() /*-{
         return this.value;
      }-*/;

      /**
       * @return Position cursor should be navigated to, relative to the
       *       beginning of the value.
       */
      public native final Position getCursorPosition() /*-{
         return this.position || {row: 0, column: 0};
      }-*/;
   }

   protected Mode()
   {
   }

   public native final CodeModel getCodeModel() /*-{
      return this.codeModel || {};
   }-*/;

   public native final FoldingRules getFoldingRules() /*-{
      return this.foldingRules;
   }-*/;

   public native final InsertChunkInfo getInsertChunkInfo() /*-{
      return this.insertChunkInfo || null;
   }-*/;
}
