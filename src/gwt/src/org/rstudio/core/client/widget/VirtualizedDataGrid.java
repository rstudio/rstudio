/*
 * VirtualizedDataGrid.java
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

import org.rstudio.core.client.theme.RStudioDataGridResources;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

// This class acts as a DOM-virtualized version of a DataGrid, effectively
// allowing the class to render large tables without overloading the DOM.
// The main requirement is that all rows within the drawn table have the
// same height.
public abstract class VirtualizedDataGrid<T> extends DataGrid<T>
{
   public class TableBuilder extends DefaultCellTableBuilder<T>
   {
      public TableBuilder(AbstractCellTable<T> cellTable)
      {
         super(cellTable);
      }
      
      @Override
      public void buildRowImpl(T data, int index)
      {
         // if this is the first row, draw padding
         if (index == 0)
            drawTopRowPadding();
         
         // draw this row if it's in the visibility range
         if (firstActiveRow_ <= index && index <= lastActiveRow_)
            super.buildRowImpl(data, index);
         
         // if this is the last row, draw padding
         if (index == lastActiveRow_)
            drawBottomRowPadding();
      }
      
      private void drawTopRowPadding()
      {
         drawPaddingRow(getTopPaddingHeight());
      }
      
      private void drawBottomRowPadding()
      {
         drawPaddingRow(getBottomPaddingHeight());
      }
      
      private void drawPaddingRow(int heightPx)
      {
         if (heightPx <= 0)
            return;
         
         TableRowBuilder tr = startRow();
         tr.attribute("height", heightPx + "px");
         for (int i = 0; i < cellTable.getColumnCount(); i++)
            tr.startTD().endTD();
         tr.endTR();
      }
   }
   
   // These two fields allow us to compute the expected
   // total size of the widget.
   public abstract int getRowHeight();
   public abstract int getTotalNumberOfRows();
   
   public VirtualizedDataGrid()
   {
      super(Integer.MAX_VALUE, RES);
      commonInit();
   }
   
   public VirtualizedDataGrid(Resources resources)
   {
      super(Integer.MAX_VALUE, resources);
      commonInit();
   }
   
   private void commonInit()
   {
      addScrollHandler(new ScrollHandler()
      {
         @Override
         public void onScroll(ScrollEvent event)
         {
            redrawTimer_.schedule(700);
         }
      });
      
      setTableBuilder(new TableBuilder(this));
      
      redrawTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            redrawIfNecessary();
         }
      };
   }
   
   @Override
   protected boolean resetFocusOnCell()
   {
      // normally, when a DataGrid is re-drawn, it
      // will also attempt to restore focus such that
      // the previously selected cell is placed into view;
      // we override this method to avoid that behavior
      return true;
   }
   
   @Override
   public void redraw()
   {
      updateActiveRows();
      super.redraw();
   }
   
   public void redrawIfNecessary()
   {
      int oldFirstActiveRow = firstActiveRow_;
      int oldLastActiveRow = lastActiveRow_;
      
      updateActiveRows();
      
      boolean changed =
            (firstActiveRow_ != oldFirstActiveRow) ||
            (lastActiveRow_ != oldLastActiveRow);
      
      if (changed)
      {
         redraw();
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               VirtualizedDataGrid.this.onResize();
            }
         });
      }
   }
   
   public HeaderPanel getHeaderPanel()
   {
      return (HeaderPanel) getWidget();
   }
   
   public ScrollPanel getScrollPanel()
   {
      return (ScrollPanel) getHeaderPanel().getContentWidget();
   }
   
   public HandlerRegistration addScrollHandler(ScrollHandler handler)
   {
      ScrollPanel panel = getScrollPanel();
      return panel.addScrollHandler(handler);
   }
   
   public final int getFirstActiveRow()
   {
      return firstActiveRow_;
   }
   
   public final int getLastActiveRow()
   {
      return lastActiveRow_;
   }
   
   public final int getTopPaddingHeight()
   {
      int pseudoRowCount = firstActiveRow_;
      return pseudoRowCount * getRowHeight();
   }
   
   public final int getBottomPaddingHeight()
   {
      int pseudoRowCount = (getTotalNumberOfRows() - lastActiveRow_) - 1;
      return pseudoRowCount * getRowHeight();
   }
   
   public final int getVerticalScrollPosition()
   {
      int tableTop =
            getTableBodyElement().getAbsoluteTop() -
            getTableHeadElement().getOffsetHeight();
      int widgetTop = getAbsoluteTop();
      return widgetTop - tableTop + 1;
   }
   
   public final void setVerticalScrollPosition(int position)
   {
      ScrollPanel panel = getScrollPanel();
      panel.setVerticalScrollPosition(position);
   }
   
   private final void updateActiveRows()
   {
      int n = getTotalNumberOfRows();
      int rowHeight = getRowHeight();
      
      // determine how much the table body element has been scrolled
      int scrollAmount = getVerticalScrollPosition();
      
      // determine the number of rows that have been scrolled
      // (this is, approximately, the first visible row)
      int numRowsScrolled = scrollAmount / rowHeight;
      
      // determine the total number of rows visible
      int rowsActive = getOffsetHeight() / rowHeight;
      
      firstActiveRow_ = Math.max(0, numRowsScrolled - ROW_PADDING);
      lastActiveRow_ = Math.min(n, numRowsScrolled + rowsActive + ROW_PADDING);
   }
   
   private int firstActiveRow_;
   private int lastActiveRow_;
   private Timer redrawTimer_;
   
   // Static Members ----
   private static final int ROW_PADDING = 200;
   private static final Resources RES = GWT.create(RStudioDataGridResources.class);
}
