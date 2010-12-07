/*
 * HistoryEntryItemCodec.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.history.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.i18n.client.DateTimeFormat;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.FastSelectTable.ItemCodec;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane.Resources;

import java.util.Date;

public class HistoryEntryItemCodec implements ItemCodec<HistoryEntry, String, Long>
{
   public HistoryEntryItemCodec(String commandClass,
                                String timestampClass,
                                boolean alwaysTimestamp,
                                boolean disclosureButton)
   {
      commandClass_ = commandClass;
      timestampClass_ = timestampClass;
      alwaysTimestamp_ = alwaysTimestamp;
      disclosureButton_ = disclosureButton;

      res_ = GWT.create(HistoryPane.Resources.class);
   }

   public TableRowElement getRowForItem(HistoryEntry entry)
   {
      TableRowElement tr = Document.get().createTRElement();
      tr.setAttribute("data-entry-id",
                      entry.getIndex() + "");
      tr.setAttribute("data-timestamp",
                      entry.getTimestamp().getTime() + "");

      TableCellElement td = Document.get().createTDElement();
      td.setColSpan(2);
      td.setClassName(commandClass_);

      DivElement div = Document.get().createDivElement();
      div.setInnerText(addBreaks(entry.getCommand()));

      td.appendChild(div);
      tr.appendChild(td);
      TableCellElement tdDiscButton = maybeCreateDisclosureButton(entry);
      if (tdDiscButton != null)
         tr.appendChild(tdDiscButton);

      return tr;
   }

   protected TableCellElement maybeCreateDisclosureButton(HistoryEntry entry)
   {
      if (!disclosureButton_)
         return null;

      TableCellElement td = Document.get().createTDElement();
      td.setClassName(res_.styles().disclosure());
      td.setVAlign("middle");

      DivElement div = Document.get().createDivElement();
      div.setClassName(res_.styles().disclosure());

      td.appendChild(div);
      return td;
   }

   protected String addBreaks(String val)
   {
      return val.replace("(", "(\u200b");
   }

   protected String stripBreaks(String val)
   {
      return val.replace("\u200b", "");
   }

   public String getOutputForRow(TableRowElement row)
   {
      return stripBreaks(row.getCells().getItem(0).getInnerText());
   }

   public Long getOutputForRow2(TableRowElement row)
   {
      return Long.parseLong(row.getAttribute("data-entry-id"));
   }

   private long getTimestampForRow(TableRowElement row)
   {
      return Long.parseLong(row.getAttribute("data-timestamp"));
   }

   public boolean isValueRow(TableRowElement row)
   {
      return !timestampClass_.equals(row.getClassName());
   }

   public boolean hasNonValueRows()
   {
      return !alwaysTimestamp_;
   }

   public void onRowsInserted(TableSectionElement tbody)
   {
      long lastTime = -1;

      Node previousSibling = tbody.getPreviousSibling();
      if (previousSibling != null
          && previousSibling.getNodeType() == Node.ELEMENT_NODE
          && ((Element)previousSibling).getTagName().equalsIgnoreCase("tbody"))
      {
         TableSectionElement prevbody = (TableSectionElement) previousSibling;
         NodeList<TableRowElement> prevrows = prevbody.getRows();
         if (prevrows.getLength() > 0)
         {
            TableRowElement lastRow = prevrows.getItem(prevrows.getLength()-1);
            if (isValueRow(lastRow))
            {
               lastTime = getTimestampForRow(lastRow);
            }
         }
      }

      int totalExtraRows = 0;
      final NodeList<TableRowElement> rows = tbody.getRows();
      for (int i = 0; i < rows.getLength(); i++)
      {
         TableRowElement row = rows.getItem(i);
         long time = getTimestampForRow(row);
         if (alwaysTimestamp_ || Math.abs(time - lastTime) > 1000*60*15)
         {
            final String formatted = formatDate(time);
            if (formatted != null)
            {
               int extraRows;
               if (alwaysTimestamp_)
                  extraRows = addTimestampCell(row, formatted);
               else
                  extraRows = addTimestampRow(row, formatted);
               i += extraRows;
               totalExtraRows += extraRows;
            }
         }
         lastTime = time;
      }

      tbody.setPropertyInt("extrarows", totalExtraRows);
   }

   public Integer logicalOffsetToPhysicalOffset(TableElement table, int offset)
   {
      NodeList<TableSectionElement> bodies = table.getTBodies();
      int skew = 0;
      int pos = 0;
      for (int i = 0; i < bodies.getLength(); i++)
      {
         TableSectionElement body = bodies.getItem(i);
         NodeList<TableRowElement> rows = body.getRows();
         int rowCount = rows.getLength();
         int extraRows = body.getPropertyInt("extrarows");
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

   private int addTimestampRow(TableRowElement row, String formatted)
   {
      Element tsRow = Document.get().createElement("tr");
      tsRow.setClassName(timestampClass_);
      tsRow.setInnerHTML("<td colspan='2'>" +
                         DomUtils.textToHtml(formatted) +
                         "</td>");
      row.getParentElement().insertBefore(tsRow, row);
      return 1;
   }

   private int addTimestampCell(TableRowElement row, String formatted)
   {
      TableCellElement cell = row.getCells().getItem(0);
      cell.setColSpan(1);
      TableCellElement tsCell = Document.get().createElement("td").cast();
      tsCell.setClassName(timestampClass_);
      tsCell.setInnerText(formatted);
      row.insertAfter(tsCell, cell);
      return 0;
   }

   private String formatDate(long time)
   {
      if (time == 0)
         return null;
      Date date = new Date(time);
      return DateTimeFormat.getShortDateTimeFormat().format(date);
   }

   private final String commandClass_;
   private final String timestampClass_;
   private final boolean alwaysTimestamp_;
   private final boolean disclosureButton_;
   private Resources res_;
}
