/*
 * Range.java
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

public class Range extends JavaScriptObject
{
   protected Range() {}
   
   public static final native Range create(int startRow,
                                           int startColumn,
                                           int endRow,
                                           int endColumn)
   /*-{
      var Range = $wnd.require('ace/range').Range;
      return new Range(startRow, startColumn, endRow, endColumn);
   }-*/;

   public static native Range fromPoints(Position start, Position end) /*-{
      var Range = $wnd.require('ace/range').Range;
      return Range.fromPoints(start, end);
   }-*/;
   
   public static native Range toOrientedRange(Range range) /*-{
      var Range = $wnd.require('ace/range').Range;
      
      // swap begin, end if Range is not 'forward'
      if (range.start.row > range.end.row || (
            range.start.row == range.end.row &&
            range.start.column > range.end.column))
      {
         return Range.fromPoints(range.end, range.start);
      }
      
      // return new range if already forward range
      return Range.fromPoints(range.start, range.end);
   }-*/;

   public final native Position getStart() /*-{
      return this.start;
   }-*/;

   public final native Position getEnd() /*-{
      return this.end;
   }-*/;
   
   public final native Position setStart(Position start) /*-{
      this.start = start;
   }-*/;
   
   public final native Position setEnd(Position end) /*-{
      this.end = end;
   }-*/;

   public final native boolean isEmpty() /*-{
      return this.isEmpty();
   }-*/;
   
   public final boolean isEqualTo(Range range)
   {
      return
            getStart().isEqualTo(range.getStart()) &&
            getEnd().isEqualTo(range.getEnd());
   }

   public final native Range extend(int row, int column) /*-{
      return this.extend(row, column);
   }-*/;
   
   public final native Range toScreenRange(EditSession session) /*-{
      return this.toScreenRange(session);
   }-*/;
   
   public final native boolean contains(int row, int column) /*-{
      return this.contains(row, column);
   }-*/;
   
   public final native boolean contains(Position position) /*-{
      return this.contains(position.row, position.column);
   }-*/;
   
   public final native boolean contains(Range range) /*-{
      return this.containsRange(range);
   }-*/;
   
   public final native boolean intersects(Range range) /*-{
      return this.intersects(range);
   }-*/;
   
   public final native boolean isMultiLine() /*-{
      return this.isMultiLine();
   }-*/;
   
   public final native boolean containsRightExclusive(Position position)
   /*-{
      
      var row = position.row;
      var column = position.column;
      
      var startRow = this.start.row;
      var endRow = this.end.row;
      
      var startColumn = this.start.column;
      var endColumn = this.end.column;
      
      if (endRow < row) return false;
      if (startRow > row) return false;
      
      if (startRow === row && endRow === row)
      {
         return column >= startColumn && 
                column <  endColumn;
      }
      
      if (startRow === row) return column >= startColumn;
      if (endRow === row)   return column <  endColumn;
      
      // shouldn't get here
      return false;
   }-*/;
   
}
