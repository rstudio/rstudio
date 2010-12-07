/*
 * GoogleSpreadsheetTable.java
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
package org.rstudio.studio.client.workbench.views.workspace.dataimport;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.DOM;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FastSelectTable;
import org.rstudio.studio.client.workbench.views.workspace.model.GoogleSpreadsheetInfo;

public class GoogleSpreadsheetTable
      extends FastSelectTable<GoogleSpreadsheetInfo, String, String>
{
   static class GoogleSpreadsheetItemCodec
         implements ItemCodec<GoogleSpreadsheetInfo,  String, String>
   {
      public GoogleSpreadsheetItemCodec(String dateStyle,
                                        String iconStyle,
                                        String iconCellStyle)
      {
         dateStyle_ = dateStyle;
         iconStyle_ = iconStyle;
         iconCellStyle_ = iconCellStyle;
      }

      public TableRowElement getRowForItem(GoogleSpreadsheetInfo entry)
      {
         TableRowElement row = DOM.createTR().cast();
         row.setAttribute("docId", entry.getResourceId());
         row.setAttribute("docName", entry.getTitle());

         TableCellElement iconCell = DOM.createTD().cast();
         iconCell.setClassName(iconCellStyle_);
         iconCell.setVAlign("middle");
         iconCell.setPropertyInt("width", 16);
         DivElement iconDiv = DOM.createDiv().cast();
         iconDiv.setClassName(iconStyle_);
         iconCell.appendChild(iconDiv);

         TableCellElement nameCell = DOM.createTD().cast();
         nameCell.setInnerText(entry.getTitle());

         TableCellElement dateCell = DOM.createTD().cast();
         dateCell.setClassName(dateStyle_);
         dateCell.setInnerText(StringUtil.formatDate(entry.getUpdated()));

         row.appendChild(iconCell);
         row.appendChild(nameCell);
         row.appendChild(dateCell);

         return row;
      }

      public void onRowsInserted(TableSectionElement tbody)
      {
      }

      public String getOutputForRow(TableRowElement row)
      {
         return row.getAttribute("docId");
      }

      public String getOutputForRow2(TableRowElement row)
      {
         return row.getAttribute("docName");
      }

      public boolean isValueRow(TableRowElement row)
      {
         return true;
      }

      public boolean hasNonValueRows()
      {
         return false;
      }

      public Integer logicalOffsetToPhysicalOffset(TableElement table,
                                                   int offset)
      {
         return offset;
      }

      private final String dateStyle_;
      private final String iconStyle_;
      private final String iconCellStyle_;
   }


   public GoogleSpreadsheetTable(String selectedStyle,
                                 String dateStyle,
                                 String iconStyle,
                                 String iconCellStyle)
   {
      super(new GoogleSpreadsheetItemCodec(dateStyle, iconStyle, iconCellStyle),
            selectedStyle,
            true,
            false);
   }
}
