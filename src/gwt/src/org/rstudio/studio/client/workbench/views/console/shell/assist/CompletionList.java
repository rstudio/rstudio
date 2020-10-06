/*
 * CompletionList.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.ScrollPanel;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

class CompletionList<TItem> extends Composite
         implements HasSelectionCommitHandlers<TItem>,
                    HasSelectionHandlers<TItem>
{
   public class GridMouseHandler implements ClickHandler, MouseMoveHandler
   {
      public void onClick(ClickEvent event)
      {
         Cell cell = grid_.getCellForEvent(event);
         if (cell != null)
         {
            int rowClicked = cell.getRowIndex();
            SelectionCommitEvent.fire(CompletionList.this, items_[rowClicked]);
         }
      }

      public void onMouseMove(MouseMoveEvent event)
      {
         if (lastMouseMoveCoordinates_ != null)
         {
            if (event.getScreenX() == lastMouseMoveCoordinates_.getX()
                  && event.getScreenY() == lastMouseMoveCoordinates_.getY())
            {
               return;
            }
         }
         lastMouseMoveCoordinates_ = Point.create(event.getScreenX(), event.getScreenY());

         if (firstEvent_)
         {
            // Want to avoid the bug where the cursor happens to be positioned
            // where the popup shows up and thus fires a mouse move event;
            // so even though the user isn't touching the mouse, the selection
            // changes.
            firstEvent_ = false;
            return;
         }

         int mousedOverRow = grid_.getRowForEvent(event);
         if (mousedOverRow >= 0)
         {
            setSelectedIndex(mousedOverRow);
         }
      }

      private boolean firstEvent_ = true;
      private Point lastMouseMoveCoordinates_;
   }

   public CompletionList(TItem[] items,
                         int visibleItems,
                         boolean asHtml,
                         boolean allowVerticalShrink)
   {
      allowVerticalShrink_ = allowVerticalShrink;
      styles_ = ConsoleResources.INSTANCE.consoleStyles();
      GridEx grid = new GridEx(items.length, 1);
      for (int i = 0; i < items.length; i++)
      {
         if (asHtml)
            grid.setHTML(i, 0, items[i].toString());
         else
            grid.setText(i, 0, items[i].toString());
      }
      grid.addClickHandler(new GridMouseHandler());
      grid.addMouseMoveHandler(new GridMouseHandler());
      grid.setStylePrimaryName(styles_.completionGrid());

      FontSizer.applyNormalFontSize(grid);

      scrollPanel_ = new ScrollPanel();
      scrollPanel_.getElement().getStyle().setProperty("overflowX", "hidden");
      scrollPanel_.add(grid);
      scrollPanel_.setHeight((visibleItems * 26) + "px");

      initWidget(scrollPanel_);
      grid_ = grid;
      items_ = items;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      int width = grid_.getOffsetWidth() + 20;
      if (maxWidthInPixels_ != null
          && maxWidthInPixels_ > 0
          && maxWidthInPixels_ < width)
         width = maxWidthInPixels_;
      scrollPanel_.setWidth(width + "px");
      if (allowVerticalShrink_ &&
          grid_.getOffsetHeight() < scrollPanel_.getOffsetHeight())
      {
         scrollPanel_.setHeight("");
      }
      grid_.setWidth("100%");
      selectNext();
   }

   public int getItemCount()
   {
      if (grid_ != null)
         return grid_.getRowCount();
      else
         return 0;
   }

   public TItem getSelectedItem()
   {
      int index = getSelectedIndex();
      if (index < 0)
         return null;
      return items_[index];
   }

   public boolean selectNext()
   {
      return moveSelection(1, true);
   }

   public boolean selectPrev()
   {
      return moveSelection(-1, true);
   }

   public boolean selectNextPage()
   {
      return moveSelection(4, false);
   }

   public boolean selectPrevPage()
   {
      return moveSelection(-4, false);
   }

   public boolean selectFirst()
   {
      return moveSelection(-getItemCount(), false);
   }

   public boolean selectLast()
   {
      return moveSelection(getItemCount(), false);
   }

   private boolean moveSelection(int offset, boolean allowWrap)
   {
      if (getItemCount() == 0)
         return false;

      int index = getSelectedIndex() + offset;
      if (allowWrap)
         index = (index + getItemCount()) % getItemCount();
      else
         index = Math.min(getItemCount() - 1, Math.max(0, index));

      setSelectedIndex(index);

      return true;
   }

   public HandlerRegistration addSelectionHandler(
         SelectionHandler<TItem> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   public HandlerRegistration addSelectionCommitHandler(
         SelectionCommitEvent.Handler<TItem> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   public HTML getDetailedInfoPane()
   {
      return null;
   }

   public int getSelectedIndex()
   {
      return selectedIndex_;
   }

   public void setSelectedIndex(int index)
   {
      if (selectedIndex_ != index)
      {
         CellFormatter cf = grid_.getCellFormatter();
         if (selectedIndex_ >= 0)
            cf.removeStyleName(selectedIndex_, 0, styles_.selected());

         selectedIndex_ = index;

         if (index >= 0)
         {
            cf.addStyleName(selectedIndex_, 0, styles_.selected());
            com.google.gwt.dom.client.Element el =
                  DomUtils.getTableCell(grid_.getElement(), index, 0);
            DomUtils.ensureVisibleVert(scrollPanel_.getElement(), el, 2);
            SelectionEvent.fire(this, getSelectedItem());
         }
      }
   }

   /**
    * Gets the rectangle of the selected row in absolute (document-relative)
    * coordinates, or null if nothing is selected.
    */
   public Rectangle getSelectionRect()
   {
      int index = getSelectedIndex();
      if (index < 0)
         return null;

      com.google.gwt.dom.client.Element el =
            DomUtils.getTableCell(grid_.getElement(), index, 0);
      return new Rectangle(el.getAbsoluteLeft(),
                      el.getAbsoluteTop(),
                      el.getOffsetWidth(),
                      el.getOffsetHeight());
   }

   public void setMaxWidth(int maxWidthInPixels)
   {
      maxWidthInPixels_ = maxWidthInPixels;
   }

   public TItem[] getItems()
   {
      return items_;
   }

   private int selectedIndex_ = -1;

   private final GridEx grid_;
   private final TItem[] items_;
   private final ScrollPanel scrollPanel_;
   private final ConsoleResources.ConsoleStyles styles_;
   private final boolean allowVerticalShrink_;
   private Integer maxWidthInPixels_;
}
