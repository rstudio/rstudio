/*
 * HistoryEntryItemCodec.java
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
package org.rstudio.studio.client.workbench.views.history.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HeaderBreaksItemCodec;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane.Resources;

import java.util.Date;

public class HistoryEntryItemCodec extends HeaderBreaksItemCodec<HistoryEntry, String, Long>
{
   public enum TimestampMode { GROUP, ITEM, NONE }

   public HistoryEntryItemCodec(String commandClass,
                                String timestampClass,
                                TimestampMode timestampMode,
                                boolean disclosureButton)
   {
      commandClass_ = commandClass;
      timestampClass_ = timestampClass;
      timestampMode_ = timestampMode;
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
      td.addClassName(ThemeStyles.INSTANCE.handCursor());
      td.setVAlign("middle");

      DivElement div = Document.get().createDivElement();
      div.setTitle("Show command in original context");
      div.setClassName(res_.styles().disclosure());
      div.addClassName(ThemeStyles.INSTANCE.handCursor());

      td.appendChild(div);
      return td;
   }

   protected String addBreaks(String val)
   {
      // This causes an extra space on Ubuntu desktop--disabling until
      // we can figure out a different way
      //return val.replace("(", "(\u200b");
      return val;
   }

   protected String stripBreaks(String val)
   {
      //return val.replace("\u200b", "");
      return val;
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
      return timestampClass_ != row.getClassName();
   }

   public boolean hasNonValueRows()
   {
      return timestampMode_ == TimestampMode.GROUP;
   }

   @Override
   protected boolean needsBreak(TableRowElement prevRow, TableRowElement row)
   {
      if (timestampMode_ == TimestampMode.ITEM)
         return true;

      if (timestampMode_ == TimestampMode.GROUP)
      {
         long lastTime = prevRow == null ? Long.MIN_VALUE
                                         : getTimestampForRow(prevRow);
         long time = getTimestampForRow(row);
         return Math.abs(time - lastTime) > 1000*60*15;
      }

      return false;
   }

   @Override
   protected int addBreak(TableRowElement row)
   {
      String formatted = formatDate(getTimestampForRow(row));
      if (timestampMode_ == TimestampMode.ITEM)
         return addTimestampCell(row, formatted);
      else
         return addTimestampRow(row, formatted);
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
      return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_SHORT).format(date);
   }

   private final String commandClass_;
   private final String timestampClass_;
   private final TimestampMode timestampMode_;
   private final boolean disclosureButton_;
   private Resources res_;
}
