/*
 * DebugFilePosition.java
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
package org.rstudio.core.client;

import com.google.gwt.core.client.JavaScriptObject;

public class DebugFilePosition extends JavaScriptObject
{
   protected DebugFilePosition() {}

   public static native DebugFilePosition create(
         int line,
         int endLine,
         int column,
         int endColumn) /*-{
      return {
         line: line, 
         end_line: endLine,
         column: column,
         end_column: endColumn
      };
   }-*/;

   public native final int getLine() /*-{
      return this.line;
   }-*/;

   public native final int getColumn() /*-{
      return this.column;
   }-*/;
   
   public native final int getEndLine() /*-{
      return this.end_line;
   }-*/;
   
   public native final int getEndColumn() /*-{
      return this.end_column; 
   }-*/;

   public final int compareTo(DebugFilePosition other)
   {
      if (other == null)
         return 1;

      int result = getLine() - other.getLine();
      if (result != 0)
         return result;

      return getColumn() - other.getColumn();
   }
   
   public native final DebugFilePosition functionRelativePosition(
         int startLine) /*-{    
      return {
         line: this.line - startLine, 
         column: this.column,
         end_line: this.end_line - startLine,
         end_column: this.end_column
      }      
   }-*/;
}
