/*
 * VirtualizedDataGrid.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.RStudioDataGridResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.ProvidesKey;

// This class acts as a DOM-virtualized version of a DataGrid, effectively
// allowing the class to render large tables without overloading the DOM.
// The main requirement is that all rows within the drawn table have the
// same height.
public abstract class VirtualizedDataGrid<T> extends RStudioDataGrid<T>
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
         {
            drawTopRowPadding();
         }
       
         // draw this row if it's in view
         if (firstActiveRow_ <= index && index <= lastActiveRow_)
         {
            // draw this row if it's in the visibility range
            super.buildRowImpl(data, index);
         }
         
         // if this is the last row, draw padding
         if (index == lastActiveRow_)
         {
            drawBottomRowPadding();
         }
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
         
         String borderColor = getBorderColor();
         
         TableRowBuilder tr = startRow();
         tr.attribute("height", heightPx + "px");
         for (int i = 0; i < cellTable.getColumnCount(); i++)
         {
            TableCellBuilder builder = tr.startTD();
            if (borderColor != null)
               builder.style().trustedBorderColor("transparent");
            builder.endTD();
         }
         tr.endTR();
      }
   }
   
   // These two fields allow us to compute the expected total size of the widget.
   public abstract double getRowHeight();
   public abstract int getTotalNumberOfRows();
   
   // This field is primarily used to ensure the top + bottom padding rows
   // are drawn with a matching style for the rest of the data grid.
   // This can return 'null' if no special styling is required.
   public abstract String getBorderColor();
   
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
   
   public VirtualizedDataGrid(Resources resources,
                              ProvidesKey<T> keyProvider)
   {
      super(Integer.MAX_VALUE, resources, keyProvider);
      commonInit();
   }
   
   
   private void commonInit()
   {
      addScrollHandler(new ScrollHandler()
      {
         @Override
         public void onScroll(ScrollEvent event)
         {
            redrawTimer_.schedule(300);
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
      // Normally, when a DataGrid is re-drawn, it will also attempt to restore
      // focus to any previously-focused cell.
      //
      // However, this also causes the currently selected cell to scroll into
      // view, which we don't necessarily want -- especially if the user is
      // currently scrolling the view away from an element with focus.
      //
      // To avoid this issue, we handle the focus reset ourselves,
      // explicitly preventing the scroll on focus.
      Element el = getKeyboardSelectedElement();
      if (el != null)
         DomUtils.setFocus(el, true);
      
      // Return 'true' to indicate our override handled the focus reset.
      return true;
   }
   
   @Override
   public void redraw()
   {
      redraw(true);
   }
   
   private void redraw(boolean update)
   {
      if (update)
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
         redraw(false);
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
      return (int) (pseudoRowCount * getRowHeight());
   }
   
   public final int getBottomPaddingHeight()
   {
      int pseudoRowCount = (getTotalNumberOfRows() - lastActiveRow_) - 1;
      return (int) (pseudoRowCount * getRowHeight());
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
      double rowHeight = getRowHeight();
      
      // determine how much the table body element has been scrolled
      int scrollAmount = getVerticalScrollPosition();
      
      // determine the number of rows that have been scrolled
      // (this is, approximately, the first visible row)
      int numRowsScrolled = (int) (scrollAmount / rowHeight);
      
      // determine the total number of rows visible
      int rowsActive = (int) (getOffsetHeight() / rowHeight);
      
      // set our active rows -- use padding to allow smoother scrolling
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
