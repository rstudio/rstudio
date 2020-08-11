/*
 * ColumnSortInfo.java
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
package org.rstudio.core.client.cellview;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortList;

public class ColumnSortInfo extends JavaScriptObject
{
   protected ColumnSortInfo()
   {
   }

   public static native ColumnSortInfo create(int columnIndex,
                                              boolean ascending) /*-{
      var sortInfo = new Object();
      sortInfo.columnIndex = columnIndex;
      sortInfo.ascending = ascending;
      return sortInfo;
   }-*/;

   public final native int getColumnIndex() /*-{
      return this.columnIndex;
   }-*/;

   public final native boolean getAscending() /*-{
      return this.ascending;
   }-*/;

   public final ColumnSortList.ColumnSortInfo toGwtSortInfo(AbstractCellTable<?> table)
   {
      return new ColumnSortList.ColumnSortInfo(
            table.getColumn(getColumnIndex()), getAscending());
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static ColumnSortInfo fromGwtSortInfo(AbstractCellTable table,
                                               ColumnSortList.ColumnSortInfo si)
   {
      return ColumnSortInfo.create(table.getColumnIndex(si.getColumn()),
                                   si.isAscending());
   }

   public static ColumnSortList setSortList(AbstractCellTable<?> table,
                                            JsArray<ColumnSortInfo> sortArray)
   {
      ColumnSortList list = table.getColumnSortList();
      list.clear();
      for (int i = 0; i < sortArray.length(); i++)
         list.insert(i, sortArray.get(i).toGwtSortInfo(table));
      return list;
   }

   @SuppressWarnings("rawtypes")
   public static JsArray<ColumnSortInfo> getSortList(CellTable table)
   {
      ColumnSortList sortList = table.getColumnSortList();
      JsArray<ColumnSortInfo> result = JsArray.createArray().cast();
      for (int i = 0; i < sortList.size(); i++)
         result.push(fromGwtSortInfo(table, sortList.get(i)));
      return result;
   }
}
