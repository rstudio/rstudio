/*
 * DirectoryContentsWidget.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.impl.FocusImpl;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.ScrollPanelWithClick;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.common.filetypes.FileIcon;

import java.util.LinkedHashMap;
import java.util.Map;

public class DirectoryContentsWidget
      extends Composite
   implements HasSelectionHandlers<FileSystemItem>,
              HasSelectionCommitHandlers<FileSystemItem>,
              HasFocusHandlers, HasBlurHandlers,
              CanFocus
{

   private static class FlexTableEx extends FlexTable
   {
      public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
      {
         return addDomHandler(handler, MouseDownEvent.getType());
      }

      public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
      {
         return addDomHandler(handler, KeyDownEvent.getType());
      }

      public HandlerRegistration addFocusHandler(FocusHandler handler)
      {
         return addDomHandler(handler, FocusEvent.getType());
      }

      public HandlerRegistration addBlurHandler(BlurHandler handler)
      {
         return addDomHandler(handler, BlurEvent.getType());
      }
   }

   public DirectoryContentsWidget(FileSystemContext context)
   {
      context_ = context;
      table_ = new FlexTableEx();
      table_.getElement().setTabIndex(0);
      table_.setCellSpacing(0);
      table_.setCellPadding(2);
      table_.setSize("100%", "100%");

      // presented to screen readers as a single-select listbox
      Roles.getListboxRole().set(table_.getElement());
      Roles.getListboxRole().setAriaLabelProperty(table_.getElement(), "Directory Contents");

      scrollPanel_ = new ScrollPanelWithClick(table_);
      scrollPanel_.setSize("100%", "100%");

      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setWidget(null);

      buffer_ = new StringBuilder();
      bufferTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            buffer_.setLength(0);
         }
      };

      initWidget(progressPanel_);

      setStylePrimaryName(styles_.contents());

      hookMouseEvents();
   }

   private void hookMouseEvents()
   {
      table_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.preventDefault();
            event.stopPropagation();

            // IE10: focusing the the table element causes the scroll panel to
            // snap it to the top, scrolling the event target offscreen. Set
            // and restore the position after focus.
            if (DomUtils.getActiveElement() != table_.getElement())
            {
               int h = scrollPanel_.getHorizontalScrollPosition();
               int v = scrollPanel_.getVerticalScrollPosition();
               focusImpl_.focus(table_.getElement());
               scrollPanel_.setHorizontalScrollPosition(h);
               scrollPanel_.setVerticalScrollPosition(v);
            }

            HTMLTable.Cell cell = table_.getCellForEvent(event);
            if (cell != null)
            {
               setSelectedRow(cell.getRowIndex());

               if (doubleClick_.checkForDoubleClick(event.getNativeEvent()))
               {
                  commitSelection(getSelectedItem());
               }
            }
            else
               setSelectedRow(null);
         }
      });
      table_.addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
         }
      });
      table_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            bufferTimer_.schedule(700);
            int keyCode = event.getNativeKeyCode();

            if (keyCode >= 'A' && keyCode <= 'Z' ||
                keyCode >= '0' && keyCode <= '9' ||
                keyCode == '.' || keyCode == '_' || keyCode == '-')
            {
               char ch = (char) keyCode;
               if (keyCode >= 'A' && keyCode <= 'Z' && !event.isShiftKeyDown())
                  ch = Character.toLowerCase(ch);

               buffer_.append(ch);
               selectBufferMatch();
               return;
            }

            switch (keyCode)
            {
               case KeyCodes.KEY_DOWN:
                  moveBy(event, 1);
                  break;
               case KeyCodes.KEY_UP:
                  moveBy(event, -1);
                  break;
               case KeyCodes.KEY_PAGEDOWN:
                  moveBy(event, 12);
                  break;
               case KeyCodes.KEY_PAGEUP:
                  moveBy(event, -12);
                  break;
               case KeyCodes.KEY_HOME:
                  event.preventDefault();
                  event.stopPropagation();
                  if (table_.getRowCount() > 0)
                     setSelectedRow(0);
                  break;
               case KeyCodes.KEY_BACKSPACE:
                  event.preventDefault();
                  event.stopPropagation();
                  onBackspace();
                  break;
               case KeyCodes.KEY_END:
                  event.preventDefault();
                  event.stopPropagation();
                  if (table_.getRowCount() > 0)
                     setSelectedRow(table_.getRowCount() - 1);
                  break;
               case KeyCodes.KEY_ENTER:
                  event.preventDefault();
                  event.stopPropagation();
                  commitSelection(getSelectedItem());
                  break;
            }
         }

         private void moveBy(KeyDownEvent event, int offset)
         {
            event.stopPropagation();
            event.preventDefault();
            moveSelection(offset);
         }
      });

      scrollPanel_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            event.preventDefault();
            event.stopPropagation();
            setSelectedRow(null);
         }
      });
      scrollPanel_.addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
         }
      });
   }

   private void commitSelection(FileSystemItem item)
   {
      buffer_.setLength(0);
      SelectionCommitEvent.fire(DirectoryContentsWidget.this, item);
   }

   private void onBackspace()
   {
      if (!items_.containsKey(".."))
         return;

      FileSystemItem item = items_.get("..");
      commitSelection(item);
   }

   private interface StringTransformer
   {
      public String transform(String string);
   }

   private static class IdentityStringTransformer implements StringTransformer
   {
      public String transform(String string) { return string; }
   }

   private static class LowerCaseStringTransformer implements StringTransformer
   {
      public String transform(String string) { return string.toLowerCase(); }
   }

   private boolean selectBufferMatchImpl(String buffer, StringTransformer transformer)
   {
      String string = transformer.transform(buffer);
      int i = 0;

      for (Map.Entry<String, FileSystemItem> entry : items_.entrySet())
      {
         String fileName = transformer.transform(entry.getKey());
         if (fileName.startsWith(string))
         {
            setSelectedRow(i);
            return true;
         }

         i++;
      }

      return false;
   }

   private void selectBufferMatch()
   {
      if (buffer_.length() == 0)
         return;

      String buffer = buffer_.toString();
      if (selectBufferMatchImpl(buffer, new IdentityStringTransformer()))
         return;

      if (selectBufferMatchImpl(buffer, new LowerCaseStringTransformer()))
         return;
   }

   private void moveSelection(int offset)
   {
      if (selectedRow_ == null)
      {
         if (table_.getRowCount() > 0)
            setSelectedRow(0);
         return;
      }

      int row = selectedRow_ + offset;
      row = Math.max(0, Math.min(table_.getRowCount()-1, row));
      setSelectedRow(row);
   }

   public void setSelectedRow(Integer row)
   {
      if (selectedRow_ != null)
      {
         table_.getRowFormatter().removeStyleName(selectedRow_, "gwt-MenuItem-selected");
         Roles.getOptionRole().removeAriaSelectedState(
               table_.getRowFormatter().getElement(selectedRow_));
         Roles.getListboxRole().removeAriaActivedescendantProperty(table_.getElement());
         selectedRow_ = null;
         selectedValue_ = null;
      }

      if (row != null && row >= 0 && row < table_.getRowCount())
      {
         selectedRow_ = row;
         table_.getRowFormatter().addStyleName(selectedRow_, "gwt-MenuItem-selected");
         Roles.getOptionRole().setAriaSelectedState(
               table_.getRowFormatter().getElement(selectedRow_), SelectedValue.TRUE);
         Roles.getListboxRole().setAriaActivedescendantProperty(
               table_.getElement(),
               Id.of(table_.getRowFormatter().getElement(selectedRow_)));
         selectedValue_ = table_.getText(row, COL_NAME);

         TableRowElement rowEl = ((TableElement)table_.getElement().cast())
               .getRows().getItem(selectedRow_);
         int horizScroll = scrollPanel_.getHorizontalScrollPosition();
         rowEl.scrollIntoView();
         scrollPanel_.setHorizontalScrollPosition(horizScroll);
      }

      SelectionEvent.fire(DirectoryContentsWidget.this,
                          getSelectedItem());
   }

   public String getSelectedValue()
   {
      return selectedValue_;
   }

   public FileSystemItem getSelectedItem()
   {
      return items_.get(getSelectedValue());
   }

   public void showProgress(boolean show)
   {
      if (show)
         progressPanel_.showProgress(300);
      else
         progressPanel_.setWidget(scrollPanel_);
   }

   public void clearContents()
   {
      Roles.getListboxRole().removeAriaActivedescendantProperty(table_.getElement());
      table_.removeAllRows();
      uniqueIdIndex_ = 0;
      items_.clear();
      selectedRow_ = null;
      selectedValue_ = null;
      scrollPanel_.scrollToTop();
      scrollPanel_.scrollToLeft();
   }

   public void setContents(FileSystemItem[] contents,
                           FileSystemItem parentDirectory)
   {
      clearContents();

      if (parentDirectory != null)
         addItem(parentDirectory,
                 "..",
                 FileIcon.PARENT_FOLDER_ICON);

      for (FileSystemItem fsi : contents)
         addItem(fsi, null, null);

      showProgress(false);
   }

   private int addItem(FileSystemItem item,
                       String customName,
                       FileIcon customIcon)
   {
      if (customName == null)
         customName = item.getName();
      if (customIcon == null)
         customIcon = context_.getIcon(item);

      items_.put(customName, item);

      int newRow = table_.insertRow(table_.getRowCount());
      Element tr = table_.getRowFormatter().getElement(newRow);
      Roles.getOptionRole().set(tr);
      tr.setId(ElementIds.ID_PREFIX + "dirContents_" + uniqueIdIndex_++);
      table_.setWidget(
            newRow,
            COL_ICON,
            customIcon.getImage());
      table_.setText(newRow, COL_NAME, customName);

      table_.getCellFormatter().setStylePrimaryName(newRow,
                                                    COL_ICON,
                                                    styles_.columnIcon());
      table_.getCellFormatter().setStylePrimaryName(newRow,
                                                    COL_NAME,
                                                    styles_.columnName());

      if (!item.isDirectory())
      {
         table_.setText(newRow,
                        COL_SIZE,
                        StringUtil.formatFileSize(item.getLength()));

         table_.setText(newRow,
                        COL_TIMESTAMP,
                        StringUtil.formatDate(item.getLastModified()));

         table_.getCellFormatter().setStylePrimaryName(newRow,
                                                       COL_SIZE,
                                                       styles_.columnSize());
         table_.getCellFormatter().setStylePrimaryName(newRow,
                                                       COL_TIMESTAMP,
                                                       styles_.columnDate());
      }
      else
      {
         ((FlexTable.FlexCellFormatter)table_.getCellFormatter()).setColSpan(
               newRow, COL_NAME, 3);
      }
      return newRow;
   }

   public Point getScrollPosition()
   {
      return Point.create(
            scrollPanel_.getHorizontalScrollPosition(),
            scrollPanel_.getVerticalScrollPosition());
   }

   public void setScrollPosition(Point p)
   {
      scrollPanel_.setVerticalScrollPosition(p.getY());
      scrollPanel_.setHorizontalScrollPosition(p.getX());
   }

   public HandlerRegistration addSelectionHandler(
         SelectionHandler<FileSystemItem> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   public HandlerRegistration addSelectionCommitHandler(
         SelectionCommitEvent.Handler<FileSystemItem> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   public void addDirectory(FileSystemItem directory)
   {
      int rowNum = addItem(directory, null, null);

      TableElement table = (TableElement) table_.getElement().cast();
      TableRowElement row = table.getRows().getItem(rowNum);
      row.scrollIntoView();
      scrollPanel_.setHorizontalScrollPosition(0);
      setSelectedRow(rowNum);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return table_.addFocusHandler(handler);
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return table_.addBlurHandler(handler);
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         focusImpl_.focus(table_.getElement());
      else
         focusImpl_.blur(table_.getElement());
   }

   public void focus()
   {
      setFocus(true);
   }

   private Map<String, FileSystemItem> items_ = new LinkedHashMap<>();
   private final DoubleClickState doubleClick_ = new DoubleClickState();
   private Integer selectedRow_;
   private String selectedValue_;
   private int uniqueIdIndex_;
   private final FlexTableEx table_;
   private final ScrollPanelWithClick scrollPanel_;
   private final SimplePanelWithProgress progressPanel_;
   private static final int COL_ICON = 0;
   private static final int COL_NAME = 1;
   private static final int COL_SIZE = 2;
   private static final int COL_TIMESTAMP = 3;
   private final FileDialogStyles styles_ = FileDialogResources.INSTANCE.styles();
   private final StringBuilder buffer_;
   private final Timer bufferTimer_;

   private final FocusImpl focusImpl_ = FocusImpl.getFocusImplForPanel();
   private final FileSystemContext context_;
}
