/*
 * RowTable.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DOMRect;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.dom.EventProperty;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitEvent.Handler;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
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
   implements HasSelectionHandlers<T>,
              HasSelectionCommitHandlers<T>
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
   
   public static enum ScrollType
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
      table_.setTabIndex(0);
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
            // blank space below a short list's rows is not a row
            Element rowEl = findRowElement(event.getNativeEvent());
            if (rowEl != null)
               SelectionCommitEvent.fire(RowTable.this, data_.get(getRowIndex(rowEl)));
         }
      }, DoubleClickEvent.getType());
      
      addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            Element rowEl = findRowElement(event.getNativeEvent());
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
                     event.stopPropagation();
                     event.preventDefault();
                     SelectionCommitEvent.fire(RowTable.this, data_.get(selectedRow_));
                  }
               }
            }

            // keyboard use means a keyboard user, even if focus arrived by
            // mouse; shown after handling the key, so that with no option
            // active yet the first Down lands on row 0
            showFocusRing();
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
      
      setFocusEventListener(table_);
      addStyleName(RES.styles().panel());
      syncEmptyState();
   }
   
   private final native void setFocusEventListener(Element table)
   /*-{
      
      var self = this;
      var handler = $entry(function(event) {
         self.@org.rstudio.core.client.widget.RowTable::onFocusBlur(*)(event);
      });
      
      table.addEventListener("focus", handler);
      table.addEventListener("blur",  handler);
      
   }-*/;
   
   private void onFocusBlur(NativeEvent event)
   {
      if (StringUtil.equals(event.getType(), "focus"))
      {
         addStyleName(RES.styles().active());
         if (matchesFocusVisible(table_))
            showFocusRing();
      }
      else if (StringUtil.equals(event.getType(), "blur"))
      {
         removeStyleName(RES.styles().active());
         removeStyleName(RES.styles().focusVisible());
         setActiveRow(-1);
         updateActiveDescendant();
      }
      else
      {
         Debug.logWarning("Unexpected event type '" + event.getType() + "'");
      }
   }
   
   // The focus ring goes on the current option rather than the table, as a
   // native listbox draws it; an empty list rings its scroll panel instead.
   // Engines without :focus-visible (HtmlUnit in unit tests) throw on the
   // selector; there the ring appears on the first keypress instead.
   private static final native boolean matchesFocusVisible(Element el)
   /*-{
      try {
         return el.matches(":focus-visible");
      } catch (e) {
         if (e.name === "SyntaxError")
            return false;
         throw e;
      }
   }-*/;
   
   // The ring and the active option come and go together, so what is shown,
   // what a screen reader announces, and where the arrow keys start agree.
   // Mouse focus shows neither: the first key press brings both.
   private void showFocusRing()
   {
      if (isFocusRingShown())
         return;
      
      addStyleName(RES.styles().focusVisible());
      activateFirstRowIfUnselected(true);
   }
   
   private boolean isFocusRingShown()
   {
      return getElement().hasClassName(RES.styles().focusVisible());
   }
   
   // Per the ARIA listbox pattern, focus lands on the first option when none
   // is selected. It becomes the active descendant without being selected,
   // since selecting it would change a dialog's chosen file or folder.
   private void activateFirstRowIfUnselected(boolean scrollIntoView)
   {
      if (selectedRow_ != -1 || data_.isEmpty())
         return;
      
      setActiveRow(0);
      updateActiveDescendant();
      if (scrollIntoView)
         scrollIntoViewIfNeeded(0);
   }
   
   private void setActiveRow(int row)
   {
      TableRowElement rowEl = getRenderedRowElement(activeRow_);
      if (rowEl != null)
         rowEl.removeClassName(RES.styles().focused());
      
      activeRow_ = row;
      rowEl = getRenderedRowElement(row);
      if (rowEl != null)
         rowEl.addClassName(RES.styles().focused());
   }
   
   // Names the current option -- the selected one, else the focused one --
   // only while virtualization renders it: the attribute must reference an
   // element in the document.
   private void updateActiveDescendant()
   {
      int row = selectedRow_ == -1 ? activeRow_ : selectedRow_;
      TableRowElement rowEl = getRenderedRowElement(row);
      if (rowEl == null)
         table_.removeAttribute("aria-activedescendant");
      else
         table_.setAttribute("aria-activedescendant", rowEl.getId());
   }
   
   // Virtualization renders only a window of rows; returns null for rows
   // outside it. Offsets skip the colgroup and top padding elements.
   private TableRowElement getRenderedRowElement(int row)
   {
      int index = row - offset_;
      if (row == -1 || index < 0 || index >= table_.getChildCount() - 3)
         return null;
      
      return table_.getChild(index + 2).cast();
   }
   
   private void syncEmptyState()
   {
      setStyleName(RES.styles().empty(), data_.isEmpty());
   }
   
   private String getRowId(int row)
   {
      return id_ + "_row_" + row;
   }
   
   public T getSelectedItem()
   {
      return selectedItem_;
   }
   
   public void redraw()
   {
      double scrollPos = getScrollTop();
      
      if (selectedRowElement_ != null)
      {
         selectedRowElement_.removeClassName(RES.styles().selected());
         selectedRowElement_.removeAttribute("aria-selected");
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
      
      int row = selectedRow_;
      if (row != -1)
      {
         selectedRow_ = -1;
         selectRow(row, ScrollType.NONE);
      }
      
      updateActiveDescendant();
   }

   public void draw(List<T> data)
   {
      draw(data, false);
   }
   
   public void draw(List<T> data, boolean clearExisting)
   {
      if (clearExisting)
         clear();

      data_ = data;
      syncEmptyState();

      buffer_.setLength(0);
      bufferTimer_.cancel();
      
      drawColumnGroups();
      
      topPaddingRowEl_ = drawPaddingRow(getTopPaddingHeight());
      topPaddingRowEl_.setAttribute("aria-hidden", "true");
      
      for (int i = 0, n = getVirtualTableRowCount(); i < n; i++)
      {
         TableRowElement rowEl = Document.get().createTRElement();
         rowEl.addClassName(RES.styles().row());
         rowEl.setAttribute("height", getRowHeight() + "px");
         rowEl.setAttribute("role", "option");
         drawRow(i + offset_, rowEl);
         table_.appendChild(rowEl);
      }
      
      bottomPaddingRowEl_ = drawPaddingRow(getBottomPaddingHeight());
      bottomPaddingRowEl_.setAttribute("aria-hidden", "true");
      
      // a redraw (e.g. a file dialog navigating) can happen while focused;
      // blur removes the ring, so it shows only while the table has focus
      if (isFocusRingShown())
         activateFirstRowIfUnselected(false);
      
      updateActiveDescendant();
   }
   
   private void drawRow(int index, TableRowElement rowEl)
   {
      T object = data_.get(index);

      // set internal attributes used for tracking a row's index in the DOM
      rowEl.setAttribute("__row", String.valueOf(index));
      rowEl.setId(getRowId(index));
      
      // redraws reuse row elements, so the class must be cleared as well as set
      if (index == activeRow_)
         rowEl.addClassName(RES.styles().focused());
      else
         rowEl.removeClassName(RES.styles().focused());

      // call back to implementing subclass to draw the row
      drawRowImpl(object, rowEl);

      // set a default title for the element if none was set during draw
      String title = rowEl.getTitle();
      if (StringUtil.isNullOrEmpty(title))
      {
         title = getKey(object);
         rowEl.setTitle(title);
      }
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

   private int getVirtualTableRowCount()
   {
      return Math.min(data_.size(), MAX_VISIBLE_ROWS);
   }
   
   private void updateOffset()
   {
      double rowHeight = getRowHeight();
      
      // determine how much the table body element has been scrolled
      double scrollAmount = getScrollTop();
      
      // determine the number of rows that have been scrolled
      // (this is, approximately, the first visible row)
      int numRowsScrolled = (int) (scrollAmount / rowHeight);

      // compute number of rows in virtual table
      int n = getVirtualTableRowCount();
      
      // set our view offset
      int offset = numRowsScrolled - (n / 2);
      
      // clamp to boundaries of view
      offset_ = MathUtil.clamp(offset, 0, data_.size() - n);
   }
   
   public void clear()
   {
      data_.clear();
      syncEmptyState();
      table_.removeAllChildren();
      SelectionEvent.fire(this, null);
      
      selectedRow_ = -1;
      selectedRowElement_ = null;
      selectedItem_ = null;
      activeRow_ = -1;
      
      offset_ = 0;
      updateActiveDescendant();
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
      // an empty list clamps some targets (Up, End, PageUp) to row 0
      if (data_.isEmpty())
         return;

      row = MathUtil.clamp(row, 0, data_.size() - 1);
      if (row == selectedRow_)
         return;
      
      setActiveRow(-1);
      
      // callers may append to the list passed to draw() and then select the
      // new row without redrawing (DirectoryContentsWidget.addDirectory)
      syncEmptyState();
      
      if (selectedRowElement_ != null)
      {
         selectedRowElement_.removeClassName(RES.styles().selected());
         selectedRowElement_.removeAttribute("aria-selected");
         selectedRowElement_ = null;
      }
      
      selectedRow_ = MathUtil.clamp(row, 0, data_.size() - 1);
      selectedItem_ = data_.get(selectedRow_);
      SelectionEvent.fire(this, getSelectedItem());
      
      // if the requested row is part of the current view,
      // apply selection styling to it
      selectedRowElement_ = getRenderedRowElement(row);
      if (selectedRowElement_ != null)
      {
         selectedRowElement_.addClassName(RES.styles().selected());
         selectedRowElement_.setAttribute("aria-selected", "true");
         DomUtils.setFocus(selectedRowElement_, true);
      }
      updateActiveDescendant();
      
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
      selectRow(getRowIndex(rowEl));
   }
   
   private Element findRowElement(NativeEvent event)
   {
      Element target = event.getEventTarget().cast();
      return DomUtils.findParentElement(target, new ElementPredicate()
      {
         @Override
         public boolean test(Element el)
         {
            return el.hasClassName(RES.styles().row());
         }
      });
   }
   
   private static int getRowIndex(Element rowEl)
   {
      return Integer.valueOf(rowEl.getAttribute("__row"));
   }
   
   private void selectOffset(NativeEvent event, int offset)
   {
      event.stopPropagation();
      event.preventDefault();
      int current = selectedRow_ == -1 ? activeRow_ : selectedRow_;
      selectRow(current + offset);
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
   
   public void setFocus(boolean focus)
   {
      if (focus)
      {
         table_.focus();
      }
      else
      {
         table_.blur();
      }
   }
   
   @Override
   public HandlerRegistration addSelectionHandler(SelectionHandler<T> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }
   
   @Override
   public HandlerRegistration addSelectionCommitHandler(Handler<T> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }
   
   
   private final String id_;
   private final TableElement table_;
   private final TableWidget widget_;
   
   private TableRowElement topPaddingRowEl_;
   private TableRowElement bottomPaddingRowEl_;
   
   // Members related to the current selection, if any.
   private int selectedRow_ = -1;
   private TableRowElement selectedRowElement_ = null;
   private T selectedItem_ = null;
   
   // The option that has focus while none is selected, or -1.
   private int activeRow_ = -1;
   
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
      String focusVisible();
      String empty();
      String selected();
      String focused();
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
