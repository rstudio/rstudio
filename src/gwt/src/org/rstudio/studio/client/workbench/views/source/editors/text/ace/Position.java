/*
 * Position.java
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

import org.rstudio.core.client.Debug;

import com.google.gwt.core.client.JavaScriptObject;

public class Position extends JavaScriptObject
{
   protected Position() {}

   public static native Position create(int row, int column) /*-{
      return {row: row, column: column};
   }-*/;
   
   public static native Position create(Position other) /*-{
      if (other === null)
        return null;
      return {row: other.row, column: other.column };
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
   
   public final boolean isEqualTo(Position other)
   {
      return compareTo(other) == 0;
   }

   public native final void setRow(int row) /*-{
      this.row = row;
   }-*/;

   public native final void setColumn(int column) /*-{
      this.column = column;
   }-*/;
   
   public native final void setPosition(Position position) /*-{
      this.row = position.row;
      this.column = position.column;
   }-*/;
   
   public final String asString()
   {
      return "[" + getRow() + ", " + getColumn() + "]";
   }
   
   public static final native String serialize(Position position) /*-{
      return position.row + "," + position.column;
   }-*/;
   
   public static final Position deserialize(String serialized)
   {
      String[] parts = serialized.split(",");
      if (parts.length < 2)
         return Position.create(0, 0);
      
      int row = 0;
      int col = 0;
      try
      {
         row = Integer.parseInt(parts[0]);
         col = Integer.parseInt(parts[1]);
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      
      return Position.create(row, col);
   }
      
}
