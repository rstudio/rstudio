/*
 * Position.java
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

public class Position extends JavaScriptObject
{
   protected Position() {}

   public static native Position create(int row, int column) /*-{
      return {row: row, column: column};
   }-*/;

   public native final int getRow() /*-{
      return this.row;
   }-*/;

   public native final int getColumn() /*-{
      return this.column;
   }-*/;

   public final int compareTo(Position other)
   {
      if (other == null)
         return 1;

      int result = getRow() - other.getRow();
      if (result != 0)
         return result;

      return getColumn() - other.getColumn();
   }

   public final boolean isBefore(Position other)
   {
      return compareTo(other) < 0;
   }

   public final boolean isBeforeOrEqualTo(Position other)
   {
      return compareTo(other) <= 0;
   }

   public final boolean isAfter(Position other)
   {
      return compareTo(other) > 0;
   }

   public final boolean isAfterOrEqualTo(Position other)
   {
      return compareTo(other) >= 0;
   }

   public native final void setRow(int row) /*-{
      this.row = row;
   }-*/;

   public native final void setColumn(int column) /*-{
      this.column = column;
   }-*/;
}
