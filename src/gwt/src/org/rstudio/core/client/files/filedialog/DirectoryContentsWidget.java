/*
 * DirectoryContentsWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DoubleClickState;
import org.rstudio.core.client.widget.ScrollPanelWithClick;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.common.filetypes.FileIconResources;

import java.util.HashMap;

public class DirectoryContentsWidget
      extends Composite
   implements HasSelectionHandlers<FileSystemItem>,
              HasSelectionCommitHandlers<FileSystemItem>,
              HasFocusHandlers, HasBlurHandlers
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

      scrollPanel_ = new ScrollPanelWithClick(table_);
      scrollPanel_.setSize("100%", "100%");

      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setWidget(null);

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

            focusImpl_.focus(table_.getElement());

            HTMLTable.Cell cell = table_.getCellForEvent(event);
            if (cell != null)
            {
               setSelectedRow(cell.getRowIndex());

               if (doubleClick_.checkForDoubleClick(event.getNativeEvent()))
               {
                  SelectionCommitEvent.fire(DirectoryContentsWidget.this,
                                            getSelectedItem());
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
            switch (event.getNativeKeyCode())
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
               case KeyCodes.KEY_END:
                  event.preventDefault();
                  event.stopPropagation();
                  if (table_.getRowCount() > 0)
                     setSelectedRow(table_.getRowCount() - 1);
                  break;
               case KeyCodes.KEY_ENTER:
                  event.preventDefault();
                  event.stopPropagation();
                  SelectionCommitEvent.fire(DirectoryContentsWidget.this,
                                            getSelectedItem());
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

   private void moveSelection(int offset)
   {
      if (selectedRow_ == null)
      {
         if (table_.getRowCount() > 0)
            setSelectedRow(0);
         return;
      }

      int row = selectedRow_.intValue() + offset;
      row = Math.max(0, Math.min(table_.getRowCount()-1, row));
      setSelectedRow(row);
   }

   public void setSelectedRow(Integer row)
   {
      if (selectedRow_ != null)
      {
         table_.getRowFormatter().removeStyleName(
               selectedRow_.intValue(),
               "gwt-MenuItem-selected");
         selectedRow_ = null;
         selectedValue_ = null;
      }

      if (row != null
          && row.intValue() >= 0
          && row.intValue() < table_.getRowCount())
      {
         selectedRow_ = row.intValue();
         table_.getRowFormatter().addStyleName(
               selectedRow_,
               "gwt-MenuItem-selected");
         selectedValue_ = table_.getText(row.intValue(), COL_NAME);

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
      table_.removeAllRows();
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
                 FileIconResources.INSTANCE.iconUpFolder());

      for (FileSystemItem fsi : contents)
         addItem(fsi, null, null);

      showProgress(false);
   }

   private int addItem(FileSystemItem item,
                       String customName,
                       ImageResource customIcon)
   {
      if (customName == null)
         customName = item.getName();
      if (customIcon == null)
         customIcon = context_.getIcon(item);

      items_.put(customName, item);
      
      int newRow = table_.insertRow(table_.getRowCount());
      table_.setWidget(
            newRow,
            COL_ICON,
            new Image(customIcon));
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
      return new Point(scrollPanel_.getHorizontalScrollPosition(),
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
         SelectionCommitHandler<FileSystemItem> handler)
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

   private HashMap<String, FileSystemItem> items_ =
         new HashMap<String, FileSystemItem>();
   private final DoubleClickState doubleClick_ = new DoubleClickState();
   private Integer selectedRow_;
   private String selectedValue_;
   private final FlexTableEx table_;
   private final ScrollPanelWithClick scrollPanel_;
   private final SimplePanelWithProgress progressPanel_;
   private static final int COL_ICON = 0;
   private static final int COL_NAME = 1;
   private static final int COL_SIZE = 2;
   private static final int COL_TIMESTAMP = 3;
   private final FileDialogStyles styles_ = FileDialogResources.INSTANCE.styles();

   private final FocusImpl focusImpl_ = FocusImpl.getFocusImplForPanel();
   private final FileSystemContext context_;
}
