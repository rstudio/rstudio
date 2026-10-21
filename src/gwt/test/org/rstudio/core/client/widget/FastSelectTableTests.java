/*
 * FastSelectTableTests.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import java.util.Arrays;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.junit.client.GWTTestCase;

public class FastSelectTableTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Items starting with "#" become header (non-value) rows; the rest are
   // value rows. Headers are taken as given rather than derived, so a test
   // controls exactly which rows exist.
   private static class Codec implements FastSelectTable.ItemCodec<String, String, String>
   {
      public TableRowElement getRowForItem(String item)
      {
         TableRowElement tr = Document.get().createTRElement();
         if (!item.startsWith("#"))
            tr.setAttribute("data-value", "true");
         tr.appendChild(Document.get().createTDElement()).setInnerText(item);
         return tr;
      }

      public void onRowsChanged(TableSectionElement tbody)
      {
      }

      public String getOutputForRow(TableRowElement row)
      {
         return row.getInnerText();
      }

      public String getOutputForRow2(TableRowElement row)
      {
         return row.getInnerText();
      }

      public boolean isValueRow(TableRowElement row)
      {
         return row.hasAttribute("data-value");
      }

      public boolean hasNonValueRows()
      {
         return true;
      }

      public Integer logicalOffsetToPhysicalOffset(TableElement table, int offset)
      {
         NodeList<TableRowElement> rows = table.getRows();
         for (int i = 0, logical = 0; i < rows.getLength(); i++)
         {
            if (isValueRow(rows.getItem(i)) && logical++ == offset)
               return i;
         }
         return null;
      }

      public Integer physicalOffsetToLogicalOffset(TableElement table, int offset)
      {
         int logical = 0;
         for (int i = 0; i < offset; i++)
         {
            if (isValueRow(table.getRows().getItem(i)))
               logical++;
         }
         return logical;
      }

      public int getLogicalRowCount(TableElement table)
      {
         return physicalOffsetToLogicalOffset(table, table.getRows().getLength());
      }
   }

   private FastSelectTable<String, String, String> createTable(String[]... bodies)
   {
      FastSelectTable<String, String, String> table =
            new FastSelectTable<>(new Codec(), "selected", true, true, "test");
      for (String[] body : bodies)
         table.addItems(Arrays.asList(body), false);
      return table;
   }

   private static String rowText(FastSelectTable<String, String, String> table)
   {
      StringBuilder text = new StringBuilder();
      NodeList<TableRowElement> rows = ((TableElement) table.getElement().cast()).getRows();
      for (int i = 0; i < rows.getLength(); i++)
         text.append(i == 0 ? "" : " ").append(rows.getItem(i).getInnerText());
      return text.toString();
   }

   public void testRemoveTopRowsSpanningSections()
   {
      // History adds a section per batch, so trimming the list after Load more
      // can ask for more rows than the top section holds.
      FastSelectTable<String, String, String> table = createTable(
            new String[] {"a1", "a2"},
            new String[] {"b1", "b2"},
            new String[] {"c1", "c2"});

      table.removeTopRows(5);

      assertEquals(1, table.getRowCount());
      assertEquals("c2", rowText(table));
   }

   public void testRemoveTopRowsBeyondEnd()
   {
      FastSelectTable<String, String, String> table = createTable(
            new String[] {"a1", "a2"},
            new String[] {"b1"});

      table.removeTopRows(10);

      assertEquals(0, table.getRowCount());
   }

   public void testRemovedHeaderNoLongerDescribesOptions()
   {
      FastSelectTable<String, String, String> table = createTable(
            new String[] {"#file", "v1", "v2"});
      TableElement el = table.getElement().cast();
      assertTrue(el.getRows().getItem(2).hasAttribute("aria-describedby"));

      // Drops the header along with v1, leaving v2 with no header above it.
      table.removeTopRows(1);

      assertEquals("v2", rowText(table));
      assertFalse(el.getRows().getItem(0).hasAttribute("aria-describedby"));
   }
}
