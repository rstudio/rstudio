/*
 * HeaderBreaksItemCodec.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.*;
import org.rstudio.core.client.widget.FastSelectTable.ItemCodec;

/**
 * Provides plumbing for item codecs that may introduce non-value "header"
 * rows based on differences between two rows.
 */
public abstract class HeaderBreaksItemCodec<T, TItemOutput, TItemOutput2>
   implements ItemCodec<T, TItemOutput, TItemOutput2>
{
   /**
    * Returns true if there should be a break introduced for "row".
    */
   protected abstract boolean needsBreak(TableRowElement prevRow,
                                         TableRowElement row);

   /**
    * Add a break for the given row. (This probably means adding a new row above
    * the given row.)
    * @param row The row for which a new break should be introduced.
    * @return Return the number of extra (non-value) rows just introduced.
    *    Make sure that your implementation of isValueRow returns false for
    *    any rows you introduce!
    */
   protected abstract int addBreak(TableRowElement row);

   public void onRowsChanged(TableSectionElement tbody)
   {
      if (!hasNonValueRows())
         return;

      TableRowElement lastRow = null;

      Node previousSibling = tbody.getPreviousSibling();
      if (previousSibling != null
          && previousSibling.getNodeType() == Node.ELEMENT_NODE
          && ((Element)previousSibling).getTagName().equalsIgnoreCase("tbody"))
      {
         TableSectionElement prevbody = (TableSectionElement) previousSibling;
         NodeList<TableRowElement> prevrows = prevbody.getRows();
         if (prevrows.getLength() > 0)
         {
            TableRowElement lastRowEl = prevrows.getItem(prevrows.getLength()-1);
            if (isValueRow(lastRowEl))
            {
               lastRow = lastRowEl;
            }
         }
      }

      int totalExtraRows = 0;
      final NodeList<TableRowElement> rows = tbody.getRows();
      for (int i = 0; i < rows.getLength(); i++)
      {
         TableRowElement row = rows.getItem(i);
         if (needsBreak(lastRow, row))
         {
            int extraRows = addBreak(row);
            i += extraRows;
            totalExtraRows += extraRows;
         }

         lastRow = row;
      }

      tbody.setPropertyInt(EXTRA_ROWS, totalExtraRows);
   }

   public Integer logicalOffsetToPhysicalOffset(TableElement table, int offset)
   {
      if (!hasNonValueRows())
         return offset;

      NodeList<TableSectionElement> bodies = table.getTBodies();
      int skew = 0;
      int pos = 0;
      for (int i = 0; i < bodies.getLength(); i++)
      {
         TableSectionElement body = bodies.getItem(i);
         NodeList<TableRowElement> rows = body.getRows();
         int rowCount = rows.getLength();
         int extraRows = body.getPropertyInt(EXTRA_ROWS);
         int max = (pos - skew) + (rowCount - extraRows);
         if (max <= offset)
         {
            // It's safe to skip this whole tbody. These are not the
            // rows we're looking for.
            pos += rowCount;
            skew += extraRows;
         }
         else
         {
            NodeList<TableRowElement> allRows = table.getRows();
            for (; pos < allRows.getLength(); pos++)
            {
               TableRowElement row = allRows.getItem(pos);
               if (!isValueRow(row))
                  skew++;
               else if (offset == (pos - skew))
                  return pos;
            }
         }
      }

      if (pos - skew == offset)
         return pos;
      else
         return null;
   }

   public Integer physicalOffsetToLogicalOffset(TableElement table, int offset)
   {
      if (!hasNonValueRows())
         return offset;

      if (offset >= table.getRows().getLength())
         return null;

      NodeList<TableSectionElement> bodies = table.getTBodies();
      int logicalOffset = 0;
      for (int i = 0; offset > 0 && i < bodies.getLength(); i++)
      {
         TableSectionElement body = bodies.getItem(i);
         NodeList<TableRowElement> rows = body.getRows();
         int rowCount = rows.getLength();
         int extraRows = body.getPropertyInt(EXTRA_ROWS);
         if (rowCount < offset)
         {
            logicalOffset += rowCount - extraRows;
            offset -= rowCount;
         }
         else
         {
            // It's in here
            for (int j = 0; offset > 0 && j < rows.getLength(); j++)
            {
               offset--;
               if (isValueRow(rows.getItem(j)))
                  logicalOffset++;
            }
         }
      }

      return logicalOffset;
   }

   public int getLogicalRowCount(TableElement table)
   {
      if (!hasNonValueRows())
         return table.getRows().getLength();

      NodeList<TableSectionElement> bodies = table.getTBodies();
      int logicalOffset = 0;
      for (int i = 0; i < bodies.getLength(); i++)
      {
         TableSectionElement body = bodies.getItem(i);
         NodeList<TableRowElement> rows = body.getRows();
         int rowCount = rows.getLength();
         int extraRows = body.getPropertyInt(EXTRA_ROWS);
         logicalOffset += rowCount - extraRows;
      }
      return logicalOffset;
   }

   private static final String EXTRA_ROWS = "extrarows";
}
