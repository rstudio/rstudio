/*
 * RowTable.java
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DOMRect;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.dom.EventProperty;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class RowTable<T> extends ScrollPanel
   implements HasSelectionHandlers<T>
{
   // Classes should implement this by adding the requisite number
   // of table cells to the provided table row element.
   public abstract void drawRowImpl(T object, TableRowElement rowEl);
   
   // The height of each row in the table. This needs to be set to
   // enable virtualization, so that the full height of the table
   // can be computed without rendering the whole table.
   public abstract double getRowHeight();
   
   // The column widths to assign to each column in the table.
   // Note the implicit requirement that this array has the same length
   // as the number of cells added in 'drawRowImpl'.
   public abstract int[] getColumnWidths();
   
   // The key associated with a particular row. This is primarily used
   // for prefix-matching selection when typing.
   public abstract String getKey(T object);
   
   private static enum ScrollType
   {
      NONE, DEFAULT, TOP, CENTER, END
   }
   
   private static final class TableWidget extends Widget
   {
      public TableWidget(TableElement table)
      {
         setElement(table);
      }
   }
   
   public RowTable(String ariaLabel)
   {
      id_ = ElementIds.getUniqueElementId(ElementIds.idSafeString(ariaLabel));
      data_ = new ArrayList<>();
      
      table_ = Document.get().createTableElement();
      table_.setWidth("100%");
      Roles.getListboxRole().set(table_);
      Roles.getListboxRole().setAriaLabelProperty(table_, ariaLabel);
      table_.addClassName(RES.styles().table());
      table_.setId(id_);
      
      scrollTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            redraw();
         }
      };
      
      buffer_ = new StringBuffer();
      bufferTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            buffer_.setLength(0);
         }
      };
      
      widget_ = new TableWidget(table_);
      setWidget(widget_);
      
      addDomHandler(new DoubleClickHandler()
      {
         @Override
         public void onDoubleClick(DoubleClickEvent event)
         {
            SelectionEvent.fire(RowTable.this, data_.get(selectedRow_));
         }
      }, DoubleClickEvent.getType());
      
      addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            Element eventTarget = event.getNativeEvent().getEventTarget().cast();
            Element rowEl = DomUtils.findParentElement(eventTarget, new ElementPredicate()
            {
               @Override
               public boolean test(Element el)
               {
                  return el.hasClassName(RES.styles().row());
               }
            });
            
            if (rowEl != null)
            {
               selectRow(rowEl);
            }
            
         }
      }, ClickEvent.getType());
      
      addDomHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent keyEvent)
         {
            NativeEvent event = keyEvent.getNativeEvent();
            int code = event.getKeyCode();
            
            int modifier = KeyboardShortcut.getModifierValue(event);
            if (modifier == KeyboardShortcut.NONE || modifier == KeyboardShortcut.SHIFT)
            {
               String key = EventProperty.key(event);
               if (key.length() == 1)
               {
                  key = event.getShiftKey() ? key.toUpperCase() : key.toLowerCase();
                  selectPrefix(event, key);
               }
               else if (code == KeyCodes.KEY_UP)
               {
                  selectOffset(event, -1);
               }
               else if (code == KeyCodes.KEY_DOWN)
               {
                  selectOffset(event, 1);
               }
               else if (code == KeyCodes.KEY_PAGEUP)
               {
                  selectOffset(event, -12);
               }
               else if (code == KeyCodes.KEY_PAGEDOWN)
               {
                  selectOffset(event, 12);
               }
               else if (code == KeyCodes.KEY_HOME)
               {
                  selectRow(event, 0);
               }
               else if (code == KeyCodes.KEY_END)
               {
                  selectRow(event, data_.size() - 1);
               }
               else if (code == KeyCodes.KEY_ENTER)
               {
                  if (selectedRow_ != -1)
                  {
                     SelectionEvent.fire(RowTable.this, data_.get(selectedRow_));
                  }
               }
            }
         }
      }, KeyDownEvent.getType());
      
      addScrollHandler(new ScrollHandler()
      {
         @Override
         public void onScroll(ScrollEvent event)
         {
            if (!scrollTimer_.isRunning())
               scrollTimer_.schedule(100);
         }
      });
      
      addDomHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            addStyleName(RES.styles().active());
         }
      }, FocusEvent.getType());
      
      addDomHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            removeStyleName(RES.styles().active());
         }
      }, BlurEvent.getType());
      
      addStyleName(RES.styles().panel());
   }
   
   public T getSelectedItem()
   {
      return selectedItem_;
   }
   
   @Override
   public HandlerRegistration addSelectionHandler(SelectionHandler<T> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }
   
   public void redraw()
   {
      double scrollPos = getScrollTop();
      
      if (selectedRowElement_ != null)
      {
         selectedRowElement_.removeClassName(RES.styles().selected());
         selectedRowElement_ = null;
      }
     
      updateOffset();
      updatePaddingRow(topPaddingRowEl_, getTopPaddingHeight());
      updatePaddingRow(bottomPaddingRowEl_, getBottomPaddingHeight());
      
      // subtract 3 for colgroup, top padding, bottom padding
      for (int i = 0; i < table_.getChildCount() - 3; i++)
      {
         // add 2 for colgroup, top padding
         TableRowElement rowEl = table_.getChild(i + 2).cast();
         rowEl.removeAllChildren();
         drawRow(i + offset_, rowEl);
      }
      
      setScrollTop(scrollPos);
      selectRowImpl(selectedRow_, ScrollType.NONE);
   }
   
   public void draw(List<T> data)
   {
      data_ = data;
      buffer_.setLength(0);
      bufferTimer_.cancel();
      
      int n = Math.min(MAX_VISIBLE_ROWS, data_.size());
      
      drawColumnGroups();
      
      topPaddingRowEl_ = drawPaddingRow(getTopPaddingHeight());
      
      for (int i = 0; i < n; i++)
      {
         TableRowElement rowEl = Document.get().createTRElement();
         rowEl.addClassName(RES.styles().row());
         rowEl.setAttribute("height", getRowHeight() + "px");
         drawRow(i + offset_, rowEl);
         table_.appendChild(rowEl);
      }
      
      bottomPaddingRowEl_ = drawPaddingRow(getBottomPaddingHeight());
   }
   
   private void drawRow(int index, TableRowElement rowEl)
   {
      T object = data_.get(index);
      rowEl.setAttribute("__row", String.valueOf(index));
      rowEl.setAttribute("title", getKey(object));
      rowEl.setId(id_ + "_row_" + index);
      drawRowImpl(object, rowEl);
   }
   
   private void drawColumnGroups()
   {
      int[] columnWidths = getColumnWidths();
      
      Element colgroupEl = Document.get().createElement("COLGROUP");
      for (int columnWidth : columnWidths)
      {
         Element colEl = Document.get().createElement("COL");
         colEl.getStyle().setWidth(columnWidth, Unit.PX);
         colgroupEl.appendChild(colEl);
      }
      table_.appendChild(colgroupEl);
   }
   
   private TableRowElement drawPaddingRow(int heightPx)
   {
      TableRowElement rowEl = Document.get().createTRElement();
      updatePaddingRow(rowEl, heightPx);
      table_.appendChild(rowEl);
      return rowEl;
   }
   
   private void updatePaddingRow(TableRowElement rowEl, int heightPx)
   {
      rowEl.setAttribute("height", heightPx + "px");
   }
 
   private int getTopPaddingHeight()
   {
      if (data_.size() < MAX_VISIBLE_ROWS)
      {
         return 0;
      }
      else
      {
         int pseudoRowCount = offset_;
         return (int) (pseudoRowCount * getRowHeight());
      }
   }
   
   private int getBottomPaddingHeight()
   {
      if (data_.size() < MAX_VISIBLE_ROWS)
      {
         return 0;
      }
      else
      {
         int pseudoRowCount = data_.size() - MAX_VISIBLE_ROWS - offset_;
         return (int) (pseudoRowCount * getRowHeight());
      }
   }
   
   private void updateOffset()
   {
      double rowHeight = getRowHeight();
      
      // determine how much the table body element has been scrolled
      double scrollAmount = getScrollTop();
      
      // determine the number of rows that have been scrolled
      // (this is, approximately, the first visible row)
      int numRowsScrolled = (int) (scrollAmount / rowHeight);
      
      // set our view offset
      int offset = numRowsScrolled - (MAX_VISIBLE_ROWS / 2);
      
      // clamp to boundaries of view
      offset_ = MathUtil.clamp(offset, 0, data_.size() - MAX_VISIBLE_ROWS);
      
   }
   
   public void clear()
   {
      data_.clear();
      table_.removeAllChildren();
      
      selectedRow_ = -1;
      selectedRowElement_ = null;
      selectedItem_ = null;
      
      offset_ = 0;
   }
   
   private void selectRow(NativeEvent event, int row)
   {
      event.stopPropagation();
      event.preventDefault();
      selectRow(row);
   }
   
   public void selectRow(int row)
   {
      selectRow(row, ScrollType.DEFAULT);
   }
   
   public void selectRow(int row, ScrollType scrollType)
   {
      row = MathUtil.clamp(row, 0, data_.size() - 1);
      if (selectedRow_ == row)
         return;
      
      selectRowImpl(row, scrollType);
   }
   
   private void selectRowImpl(int row, ScrollType scrollType)
   {
      if (selectedRowElement_ != null)
      {
         selectedRowElement_.removeClassName(RES.styles().selected());
         selectedRowElement_.removeAttribute("aria-selected");
         selectedRowElement_ = null;
      }
      
      selectedRow_ = row;
      selectedItem_ = data_.get(selectedRow_);
      
      // check if the requested row is part of the current view;
      // if so, apply selection styling to it. note that we need
      // to tweak offsets to handle colgroup + padding elements
      int index = row - offset_;
      if (index >= 0 && index <= table_.getChildCount() - 3)
      {
         selectedRowElement_ = table_.getChild(index + 2).cast();
         selectedRowElement_.addClassName(RES.styles().selected());
         selectedRowElement_.setAttribute("aria-selected", "true");
         table_.setAttribute("aria-activedescendant", selectedRowElement_.getId());
         DomUtils.setFocus(selectedRowElement_, true);
      }
      
      if (scrollType != ScrollType.NONE)
      {
         if (selectedRowElement_ != null)
         {
            if (scrollType == ScrollType.DEFAULT)
            {
               selectedRowElement_.scrollIntoView();
            }
            else
            {
               String behaviour = scrollType.toString().toLowerCase();
               scrollIntoView(selectedRowElement_, behaviour);
            }
            
         }
         else
         {
            scrollIntoViewIfNeeded(selectedRow_);
         }
      }
   }
   
   private void selectRow(Element rowEl)
   {
      String row = rowEl.getAttribute("__row");
      selectRow(Integer.valueOf(row));
   }
   
   private void selectOffset(NativeEvent event, int offset)
   {
      event.stopPropagation();
      event.preventDefault();
      selectRow(selectedRow_ + offset);
   }
   
   private void selectPrefix(NativeEvent event, String ch)
   {
      event.stopPropagation();
      event.preventDefault();
      
      buffer_.append(ch);
      bufferTimer_.schedule(BUFFER_TIMEOUT_MS);
      
      String buffer = buffer_.toString();
      
      for (int row = 0, n = data_.size(); row < n; row++)
      {
         T object = data_.get(row);
         String key = getKey(object);
         if (key.startsWith(buffer))
         {
            selectRow(row, ScrollType.CENTER);
            return;
         }
      }
      
      for (int row = 0, n = data_.size(); row < n; row++)
      {
         T object = data_.get(row);
         String key = getKey(object);
         if (key.toLowerCase().startsWith(buffer))
         {
            selectRow(row, ScrollType.CENTER);
            return;
         }
      }
   }
   
   private void scrollIntoViewIfNeeded(int row)
   {
      // Figure out what rows are currently in view.
      DOMRect panelBounds = DomUtils.getBoundingClientRect(getElement());
      
      int numberOfRowsAbove = (int) (getScrollTop() / getRowHeight());
      int numberOfRowsInView = (int) (panelBounds.getHeight() / getRowHeight());
      
      boolean isVisible =
            numberOfRowsAbove <= row &&
            row <= numberOfRowsAbove + numberOfRowsInView;
      
      if (!isVisible)
      {
         double scrollPos =
               (row * getRowHeight()) -
               (panelBounds.getHeight() / 2.0) +
               (getRowHeight() / 2.0);
         
         setScrollTop(scrollPos);
      }
   }
   
   private double getScrollTop()
   {
      return getScrollTopImpl(getElement());
   }
   
   private void setScrollTop(double scrollTop)
   {
      setScrollTopImpl(getElement(), scrollTop);
   }
   
   private static final native double getScrollTopImpl(Element el)
   /*-{
      return el.scrollTop;
   }-*/;
   
   private static final native double setScrollTopImpl(Element el, double scrollTop)
   /*-{
      el.scrollTop = scrollTop;
   }-*/;
   
   private static final native void scrollIntoView(Element el, String behavior)
   /*-{
      el.scrollIntoView({
         block: behavior
      });
   }-*/;
   
   private final String id_;
   private final TableElement table_;
   private final TableWidget widget_;
   
   private TableRowElement topPaddingRowEl_;
   private TableRowElement bottomPaddingRowEl_;
   
   // Members related to the current selection, if any.
   private int selectedRow_ = -1;
   private TableRowElement selectedRowElement_ = null;
   private T selectedItem_ = null;
   
   // Members related to the current rows in view, used for virtualization.
   private List<T> data_;
   private final Timer scrollTimer_;
   private int offset_ = 0;
   
   // Members related to prefix search when typing.
   private final StringBuffer buffer_;
   private final Timer bufferTimer_;
   
   
   private static final int MAX_VISIBLE_ROWS = 500;
   private static final int BUFFER_TIMEOUT_MS = 700;
   
      
   // Boilerplate ----
   public interface Styles extends CssResource
   {
      String panel();
      String table();
      String row();
      
      String active();
      String selected();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("RowTable.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   
}
