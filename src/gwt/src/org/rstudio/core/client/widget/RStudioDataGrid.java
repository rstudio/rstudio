/*
 * RStudioDataGrid.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.theme.RStudioDataGridResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.Range;

public class RStudioDataGrid<T> extends DataGrid<T>
{
   public RStudioDataGrid()
   {
      super(Integer.MAX_VALUE, RES);
   }
   
   public RStudioDataGrid(Resources resources)
   {
      super(Integer.MAX_VALUE, resources);
   }
   
   public HandlerRegistration addScrollHandler(ScrollHandler handler)
   {
      HeaderPanel header = (HeaderPanel) getWidget();
      ScrollPanel content = (ScrollPanel) header.getContentWidget();
      return content.addScrollHandler(handler);
   }
   
   public final Range getVisibleRows()
   {
      TableSectionElement tableBody = getTableBodyElement();
      TableSectionElement tableHead = getTableHeadElement();
      NodeList<TableRowElement> rowEls = tableBody.getRows();
      
      // get number of rows (bail if we have none)
      int numRows = rowEls.getLength();
      if (numRows == 0)
         return new Range(0, 0);
      
      // get the height of the first row, assuming
      // all rows have the same height
      TableRowElement firstRow = rowEls.getItem(0);
      int rowHeight = firstRow.getOffsetHeight();
      
      // get vertical dimensions of widget itself
      int widgetSize = getOffsetHeight();
      
      // determine how much the table body element has been scrolled
      int tableTop = tableBody.getAbsoluteTop() - tableHead.getOffsetHeight();
      int widgetTop = getAbsoluteTop();
      int scrollAmount = widgetTop - tableTop;
      
      // determine the number of rows that have been scrolled
      // (this is, approximately, the first visible row)
      int numRowsScrolled = scrollAmount / rowHeight;
      
      // determine the total number of rows visible
      int rowsVisible = getOffsetHeight() / rowHeight;
      
      Debug.logToRConsole(
            "Number of rows: " + numRows + "\n" +
            "Widget size: " + widgetSize + "\n" +
            "Widget top: " + getAbsoluteTop() + "\n" +
            "Table top: " + tableTop + "\n" +
            "Scroll amount: " + scrollAmount + "\n" +
            "Scrolled rows: " + numRowsScrolled + "\n" +
            "Visible rows: " + rowsVisible + "\n");
      
      return new Range(numRowsScrolled, rowsVisible);
   }
   
   private static final Resources RES = GWT.create(RStudioDataGridResources.class);
}
