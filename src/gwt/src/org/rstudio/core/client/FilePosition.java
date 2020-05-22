/*
 * FilePosition.java
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

public class FilePosition extends JavaScriptObject
{
   protected FilePosition() {}

   public static native FilePosition create(int line, int column) /*-{
      return {line: line, column: column};
   }-*/;

   public native final int getLine() /*-{
      return this.line;
   }-*/;

   public native final int getColumn() /*-{
      return this.column;
   }-*/;

   public final int compareTo(FilePosition other)
   {
      if (other == null)
         return 1;

      int result = getLine() - other.getLine();
      if (result != 0)
         return result;

      return getColumn() - other.getColumn();
   }
}
