/*
 * FastSelectTable.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.core.client.widget.events.SelectionChangedHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FastSelectTable<TItemInput, TItemOutput, TItemOutput2> extends Widget
   implements HasAllMouseHandlers, HasClickHandlers, HasAllKeyHandlers
{
   public interface ItemCodec<T, TItemOutput, TItemOutput2>
   {
      TableRowElement getRowForItem(T entry);
      void onRowsChanged(TableSectionElement tbody);
      TItemOutput getOutputForRow(TableRowElement row);
      TItemOutput2 getOutputForRow2(TableRowElement row);
      boolean isValueRow(TableRowElement row);
      boolean hasNonValueRows();

      Integer logicalOffsetToPhysicalOffset(TableElement table, int offset);
      Integer physicalOffsetToLogicalOffset(TableElement table, int offset);
      int getLogicalRowCount(TableElement table);
   }

   public FastSelectTable(ItemCodec<TItemInput, TItemOutput, TItemOutput2> codec,
                          String selectedClassName,
                          boolean focusable,
                          boolean allowMultiSelect)
   {
      codec_ = codec;
      selectedClassName_ = selectedClassName;
      focusable_ = focusable;
      allowMultiSelect_ = allowMultiSelect;

      table_ = Document.get().createTableElement();
      if (focusable_)
         table_.setTabIndex(0);
      table_.setCellPadding(0);
      table_.setCellSpacing(0);
      table_.setBorder(0);
      table_.getStyle().setCursor(Cursor.DEFAULT);
      setElement(table_);

      addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            if (event.getNativeButton() != NativeEvent.BUTTON_LEFT)
               return;

            event.preventDefault();

            NativeWindow.get().focus();
            DomUtils.setActive(getElement());

            Element cell = getEventTargetCell((Event) event.getNativeEvent());
            if (cell == null)
               return;
            TableRowElement row = (TableRowElement) cell.getParentElement();
            if (codec_.isValueRow(row))
               handleRowClick(event, row);
         }
      });
      addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.preventDefault();
         }
      });

      addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            handleKeyDown(event);
         }
      });
   }

   public void setCellPadding(int padding)
   {
      table_.setCellPadding(padding);
   }

   public void setCellSpacing(int spacing)
   {
      table_.setCellSpacing(spacing);
   }

   public void setOwningScrollPanel(ScrollPanel scrollPanel)
   {
      scrollPanel_ = scrollPanel;
   }

   private void handleRowClick(MouseDownEvent event, TableRowElement row)
   {
      int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());
      modifiers &= ~KeyboardShortcut.ALT; // ALT has no effect

      if (!allowMultiSelect_)
         modifiers = KeyboardShortcut.NONE;

      // We'll treat Ctrl and Meta as equivalent--and normalize to Ctrl.
      if (KeyboardShortcut.META == (modifiers & KeyboardShortcut.META))
         modifiers |= KeyboardShortcut.CTRL;
      modifiers &= ~KeyboardShortcut.META;

      if (modifiers == KeyboardShortcut.NONE)
      {
         // Select only the target row
         clearSelection();
         setSelected(row, true);
      }
      else if (modifiers == KeyboardShortcut.CTRL)
      {
         // Toggle the target row
         setSelected(row, !isSelected(row));
      }
      else
      {
         // SHIFT or CTRL+SHIFT

         int target = row.getRowIndex();
         Integer min = null;
         Integer max = null;
         for (TableRowElement selectedRow : selectedRows_)
         {
            if (min == null)
               min = selectedRow.getRowIndex();
            max = selectedRow.getRowIndex();
         }

         int offset; // selection offset
         int length;   // selection length

         if (min == null)
         {
            // Nothing is selected
            offset = target;
            length = 1;
         }
         else if (target < min)
         {
            // Select target..max
            offset = target;
            length = max - target + 1;
         }
         else if (target > max)
         {
            offset = min;
            length = target - min + 1;
         }
         else
         {
            // target is in between min and max
            if (modifiers == (KeyboardShortcut.CTRL | KeyboardShortcut.SHIFT))
            {
               offset = min;
               length = target - min + 1;
            }
            else
            {
               offset = target;
               length = 1;
            }
         }

         clearSelection();
         if (length > 0)
         {
            setSelectedPhysical(offset, length, true);
         }
      }
   }

   private void handleKeyDown(KeyDownEvent event)
   {
      int modifiers = KeyboardShortcut.getModifierValue(event.getNativeEvent());
      switch (event.getNativeKeyCode())
      {
         case KeyCodes.KEY_UP:
         case KeyCodes.KEY_DOWN:
            break;
         default:
            return;
      }

      if (!allowMultiSelect_)
         modifiers = KeyboardShortcut.NONE;

      event.preventDefault();
      event.stopPropagation();

      switch (modifiers)
      {
         case 0:
         case KeyboardShortcut.SHIFT:
            break;
         default:
            return;
      }

      sortSelectedRows();
      boolean clearSelection = modifiers != KeyboardShortcut.SHIFT;

      switch (event.getNativeKeyCode())
      {
         case KeyCodes.KEY_UP:
         {
            selectPreviousRow(clearSelection);
            break;
         }
         case KeyCodes.KEY_DOWN:
         {
            selectNextRow(clearSelection);
            break;
         }
      }
   }
   
   private void selectPreviousRow(boolean clearSelection)
   {
      int min = selectedRows_.size() > 0
            ? selectedRows_.get(0).getRowIndex()
            : table_.getRows().getLength();
            
      Integer row = findNextValueRow(min, true);
      if (row != null)
      {
         if (clearSelection)
            clearSelection();
         setSelectedPhysical(row, 1, true);
         ensureRowVisible(row);
      }
   }
   
   public void selectPreviousRow()
   {
      selectPreviousRow(true);
   }
   
   private void selectNextRow(boolean clearSelection)
   {
      int max = selectedRows_.size() > 0
            ? selectedRows_.get(selectedRows_.size() - 1).getRowIndex()
            : -1;
            
      Integer row = findNextValueRow(max, false);
      if (row != null)
      {
         if (clearSelection)
            clearSelection();
         setSelectedPhysical(row, 1, true);
         ensureRowVisible(row);
      }
   }
   
   public void selectNextRow()
   {
      selectNextRow(true);
   }

   private void ensureRowVisible(final int row)
   {
      if (scrollPanel_ != null)
         DomUtils.ensureVisibleVert(scrollPanel_.getElement(), getRow(row), 0);
   }

   private Integer findNextValueRow(int physicalRowIndex, boolean up)
   {
      int limit = up ? -1 : table_.getRows().getLength();
      int increment = up ? -1 : 1;
      for (int i = physicalRowIndex + increment; i != limit; i += increment)
      {
         if (codec_.isValueRow(getRow(i)))
            return i;
      }
      return null;
   }

   public void clearSelection()
   {
      while (selectedRows_.size() > 0)
         setSelected(selectedRows_.get(0), false);
   }

   public void addItems(Iterable<TItemInput> items, boolean top)
   {
      TableSectionElement tbody = Document.get().createTBodyElement();
      for (TItemInput item : items)
         tbody.appendChild(codec_.getRowForItem(item));
      if (top)
         addToTop(tbody);
      else
         getElement().appendChild(tbody);

      codec_.onRowsChanged(tbody);
   }

   protected void addToTop(TableSectionElement tbody)
   {
      getElement().insertFirst(tbody);
   }

   public void clear()
   {
      table_.setInnerText("");
      selectedRows_.clear();
   }
   
   public void focus()
   {
      if (focusable_)
         table_.focus();
   }
   
   public int getRowCount()
   {
      return codec_.getLogicalRowCount(table_);
   }
   
   public void removeTopRows(int rowCount)
   {
      if (rowCount <= 0)
         return;

      NodeList<TableSectionElement> tBodies = table_.getTBodies();
      for (int i = 0; i < tBodies.getLength(); i++)
      {
         rowCount = removeTopRows(tBodies.getItem(i), rowCount);
         if (rowCount == 0)
            return;
      }
   }

   private int removeTopRows(TableSectionElement tbody, int rowCount)
   {
      while (rowCount > 0 && tbody.getRows().getLength() >= 0)
      {
         TableRowElement topRow = tbody.getRows().getItem(0);
         if (codec_.isValueRow(topRow))
            rowCount--;
         selectedRows_.remove(topRow);
         topRow.removeFromParent();
      }

      if (tbody.getRows().getLength() > 0)
         codec_.onRowsChanged(tbody);
      else
         tbody.removeFromParent();

      return rowCount;
   }

   public ArrayList<Integer> getSelectedRowIndexes()
   {
      sortSelectedRows();

      ArrayList<Integer> results = new ArrayList<Integer>();
      for (TableRowElement row : selectedRows_)
         results.add(codec_.physicalOffsetToLogicalOffset(table_,
                                                          row.getRowIndex()));
      return results;
   }
   
   private boolean isSelected(TableRowElement tr)
   {
      return tr.getClassName().contains(selectedClassName_);
   }

   @Deprecated
   public void setSelected(int row, boolean selected)
   {
      setSelected(getRow(row), selected);
   }

   public void setSelected(int offset, int length, boolean selected)
   {
      if (codec_.hasNonValueRows())
      {
         // If the codec might have stuck in some non-value rows, we need
         // to translate the given offset/length to the actual row
         // offset/length, which may be different (greater).

         Integer start = codec_.logicalOffsetToPhysicalOffset(table_, offset);
         Integer end = codec_.logicalOffsetToPhysicalOffset(table_,
                                                            offset + length);

         if (start == null || end == null)
         {
            return;
         }

         offset = start;
         length = end - start;
      }

      setSelectedPhysical(offset, length, selected);
   }

   private void setSelectedPhysical(int offset, int length, boolean selected)
   {
      for (int i = 0; i < length; i++)
         setSelected(getRow(offset + i), selected);
   }

   public void setSelected(TableRowElement row, boolean selected)
   {
      try
      {
         if (row.getParentElement().getParentElement() != table_)
            return;
      }
      catch (NullPointerException npe)
      {
         return;
      }

      boolean isCurrentlySelected = isSelected(row);
      if (isCurrentlySelected == selected)
         return;

      if (selected && !codec_.isValueRow(row))
         return;

      setStyleName(row, selectedClassName_, selected);
      if (selected)
         selectedRows_.add(row);
      else
         selectedRows_.remove(row);

      if (selected && !allowMultiSelect_)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               fireEvent(new SelectionChangedEvent());
            }
         });
      }
   }

   public ArrayList<TItemOutput> getSelectedValues()
   {
      sortSelectedRows();

      ArrayList<TItemOutput> results = new ArrayList<TItemOutput>();
      for (TableRowElement row : selectedRows_)
         results.add(codec_.getOutputForRow(row));
      return results;
   }

   private void sortSelectedRows()
   {
      Collections.sort(selectedRows_, new Comparator<TableRowElement>()
      {
         public int compare(TableRowElement r1, TableRowElement r2)
         {
            return r1.getRowIndex() - r2.getRowIndex();
         }
      });
   }

   public ArrayList<TItemOutput2> getSelectedValues2()
   {
      sortSelectedRows();

      ArrayList<TItemOutput2> results = new ArrayList<TItemOutput2>();
      for (TableRowElement row : selectedRows_)
         results.add(codec_.getOutputForRow2(row));
      return results;
   }

   public boolean moveSelectionUp()
   {
      if (selectedRows_.size() == 0)
         return false;

      sortSelectedRows();
      int top = selectedRows_.get(0).getRowIndex();

      NodeList<TableRowElement> rows = table_.getRows();
      TableRowElement rowToSelect = null;
      while (--top >= 0)
      {
         TableRowElement row = rows.getItem(top);
         if (codec_.isValueRow(row))
         {
            rowToSelect = row;
            break;
         }
      }
      if (rowToSelect == null)
         return false;

      clearSelection();
      setSelected(rowToSelect, true);
      return true;
   }

   public boolean moveSelectionDown()
   {
      if (selectedRows_.size() == 0)
         return false;

      sortSelectedRows();
      int bottom = selectedRows_.get(selectedRows_.size() - 1).getRowIndex();

      NodeList<TableRowElement> rows = table_.getRows();
      TableRowElement rowToSelect = null;
      while (++bottom < rows.getLength())
      {
         TableRowElement row = rows.getItem(bottom);
         if (codec_.isValueRow(row))
         {
            rowToSelect = row;
            break;
         }
      }
      if (rowToSelect == null)
         return false;

      clearSelection();
      setSelected(rowToSelect, true);
      return true;
   }

   private TableRowElement getRow(int row)
   {
      return (TableRowElement) table_.getRows().getItem(row).cast();
   }

   public TableRowElement getTopRow()
   {
      if (table_.getRows().getLength() > 0)
         return getRow(0);
      else
         return null;
   }

   public ArrayList<TableRowElement> getSelectedRows()
   {
      return new ArrayList<TableRowElement>(selectedRows_);
   }

   public Rectangle getSelectionRect()
   {
      if (selectedRows_.size() == 0)
         return null;

      sortSelectedRows();

      TableRowElement first = selectedRows_.get(0);
      TableRowElement last = selectedRows_.get(selectedRows_.size() - 1);
      int top = first.getOffsetTop();
      int bottom = last.getOffsetTop() + last.getOffsetHeight();
      int left = first.getOffsetLeft();
      int width = first.getOffsetWidth();
      return new Rectangle(left, top, width, bottom - top);
   }

   protected Element getEventTargetCell(Event event) {
     Element td = DOM.eventGetTarget(event);
     for (; td != null; td = DOM.getParent(td)) {
       // If it's a TD, it might be the one we're looking for.
       if (td.getPropertyString("tagName").equalsIgnoreCase("td")) {
         // Make sure it's directly a part of this table before returning
         // it.
         
         Element tr = td.getParentElement();
         Element body = tr.getParentElement();
         Element table = body.getParentElement();
         if (table == getElement()) {
           return td;
         }
       }
       // If we run into this table's body, we're out of options.
       if (td == getElement()) {
         return null;
       }
     }
     return null;
   }

   public HandlerRegistration addMouseUpHandler(MouseUpHandler handler)
   {
      return addDomHandler(handler, MouseUpEvent.getType());
   }

   public HandlerRegistration addMouseOutHandler(MouseOutHandler handler)
   {
      return addDomHandler(handler, MouseOutEvent.getType());
   }

   public HandlerRegistration addMouseOverHandler(MouseOverHandler handler)
   {
      return addDomHandler(handler, MouseOverEvent.getType());
   }

   public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler)
   {
      return addDomHandler(handler, MouseWheelEvent.getType());
   }

   public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
   {
      return addDomHandler(handler, MouseDownEvent.getType());
   }

   public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler)
   {
      return addDomHandler(handler, MouseMoveEvent.getType());
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public HandlerRegistration addKeyUpHandler(KeyUpHandler handler)
   {
      return addDomHandler(handler, KeyUpEvent.getType());
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return addDomHandler(handler, KeyDownEvent.getType());
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return addDomHandler(handler, KeyPressEvent.getType());
   }

   public HandlerRegistration addSelectionChangedHandler(
         SelectionChangedHandler handler)
   {
      assert !allowMultiSelect_ : "Selection changed event will only fire " +
                                  "if multiselect is disabled";
      return addHandler(handler, SelectionChangedEvent.TYPE);
   }

   private final ArrayList<TableRowElement> selectedRows_ = new ArrayList<TableRowElement>();
   private final ItemCodec<TItemInput, TItemOutput, TItemOutput2> codec_;
   private final TableElement table_;
   private final String selectedClassName_;
   private final boolean allowMultiSelect_;
   private ScrollPanel scrollPanel_;
   private final boolean focusable_;
}