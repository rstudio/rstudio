/*
 * DirectoryContentsWidget.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.ScrollPanelWithClick;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.core.client.widget.VirtualizedDataGrid;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileIconResourceCell;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;

import java.util.List;

public class DirectoryContentsWidget extends Composite
   implements HasSelectionHandlers<FileSystemItem>,
              HasSelectionCommitHandlers<FileSystemItem>,
              CanFocus
{
   private static final ProvidesKey<FileSystemItem> KEY_PROVIDER =
      new ProvidesKey<FileSystemItem>() {
         @Override
         public Object getKey(FileSystemItem item)
         {
            return item.getPath();
         }
   };
   
   public DirectoryContentsWidget(FileSystemContext context)
   {
      dataProvider_ = new ListDataProvider<>();
      table_ = new VirtualizedDataGrid<FileSystemItem>(DirectoryContentsWidgetDataGridResources.INSTANCE, KEY_PROVIDER)
      {
         @Override
         public double getRowHeight()
         {
            return 22.5;
         }

         @Override
         public int getTotalNumberOfRows()
         {
            return dataProvider_.getList().size();
         }

         @Override
         public String getBorderColor()
         {
            return "transparent";
         }
      };
      
      table_.getElement().getStyle().setBackgroundColor("white");
      table_.getElement().setTabIndex(0);
      table_.setSize("500px", "298px");
      
      table_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
         }
      });
      
      table_.addDomHandler(new DoubleClickHandler()
      {
         @Override
         public void onDoubleClick(DoubleClickEvent event)
         {
            int row = table_.getKeyboardSelectedRow();
            FileSystemItem item = dataProvider_.getList().get(row);
            commitSelection(item);
         }
      }, DoubleClickEvent.getType());
      
      // Add a 'global' click handler that performs a row selection regardless
      // of the cell clicked (it seems GWT clicks can be 'fussy' about whether
      // you click on the contents of a cell vs. the '<td>' element itself)
      table_.addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            Element el = event.getNativeEvent().getEventTarget().cast();
            Element rowEl = DomUtils.findParentElement(el, new ElementPredicate()
            {
               @Override
               public boolean test(Element el)
               {
                  return el.getTagName().toLowerCase().equals("tr");
               }
            });
            
            if (rowEl != null && rowEl.hasAttribute("__gwt_row"))
            {
               int row = StringUtil.parseInt(rowEl.getAttribute("__gwt_row"), -1);
               if (row != -1)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  table_.setKeyboardSelectedRow(row);
                  table_.setKeyboardSelectedColumn(0);
                  return;
               }
            }
            
            // If no row was clicked (e.g. the user clicked into empty space in the table)
            // then just drive focus to whatever row is currently selected, or the first
            // row if nothing else is available.
            int row = table_.getKeyboardSelectedRow();
            table_.setKeyboardSelectedRow(row == -1 ? 0 : row);
         }
      }, ClickEvent.getType());
      
      table_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent keyEvent)
         {
            bufferTimer_.schedule(BUFFER_LIFETIME_MS);
            
            NativeEvent event = keyEvent.getNativeEvent();
            int code = event.getKeyCode();
            int modifiers = KeyboardShortcut.getModifierValue(event);
            if (modifiers != 0 && modifiers != KeyboardShortcut.SHIFT)
               return;

            if (code == KeyCodes.KEY_ENTER)
            {
               int row = table_.getKeyboardSelectedRow();
               if (row != -1)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  commitSelection(getSelectedItem());
               }
            }
            else if (code == KeyCodes.KEY_BACKSPACE)
            {
               if (parentDirectory_ != null)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  commitSelection(parentDirectory_);
               }
            }
            else if (code >= 32 && code <= 126)
            {
               event.stopPropagation();
               event.preventDefault();
               selectBufferMatch((char) code);
            }

         }
      });
      
      dataProvider_.addDataDisplay(table_);
      
      iconColumn_ = new Column<FileSystemItem, FileIcon>(new FileIconResourceCell())
      {
         @Override
         public FileIcon getValue(FileSystemItem item)
         {
            return registry_.getIconForFile(item);
         }
         
         private final FileTypeRegistry registry_ = RStudioGinjector.INSTANCE.getFileTypeRegistry();
      };
      
      nameColumn_ = new TextColumn<FileSystemItem>()
      {
         @Override
         public String getValue(FileSystemItem item)
         {
            return item.getName();
         }
      };
      
      sizeColumn_ = new TextColumn<FileSystemItem>()
      {
         @Override
         public String getValue(FileSystemItem item)
         {
            if (item.isDirectory())
            {
               return "";
            }
            else
            {
               return StringUtil.formatFileSize(item.getLength());
            }
         }
      };
      
      dateColumn_ = new TextColumn<FileSystemItem>()
      {
         @Override
         public String getValue(FileSystemItem item)
         {
            if (item.isDirectory())
            {
               return "";
            }
            else
            {
               return StringUtil.formatDate(item.getLastModified());
            }
         }
      };
      
      iconColumn_.setCellStyleNames(styles_.columnIcon());
      table_.addColumn(iconColumn_);
      table_.setColumnWidth(iconColumn_, "24px");
      
      nameColumn_.setCellStyleNames(styles_.columnName());
      table_.addColumn(nameColumn_);
      table_.setColumnWidth(nameColumn_, "208px");
      
      sizeColumn_.setCellStyleNames(styles_.columnSize());
      table_.addColumn(sizeColumn_);
      table_.setColumnWidth(sizeColumn_, "108px");
      
      dateColumn_.setCellStyleNames(styles_.columnDate());
      table_.addColumn(dateColumn_);
      table_.setColumnWidth(dateColumn_, "158px");

      // presented to screen readers as a single-select listbox
      Roles.getListboxRole().set(table_.getElement());
      Roles.getListboxRole().setAriaLabelProperty(table_.getElement(), constants_.directoryContentsLabel());

      scrollPanel_ = new ScrollPanelWithClick(table_)
      {
         @Override
         public void onBrowserEvent(Event event)
         {
            switch (event.getTypeInt())
            {
            case Event.ONKEYDOWN:
            case Event.ONKEYPRESS:
            case Event.ONKEYUP:
               DomEvent.fireNativeEvent(event, table_);
               return;
            }
            super.onBrowserEvent(event);
         }
      };
      
      scrollPanel_.setSize("100%", "100%");

      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setSize("100%", "300px");
      progressPanel_.setWidget(null);
      progressPanel_.setInheritEditorTheme(false);

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
   }

   private void commitSelection(FileSystemItem item)
   {
      if (StringUtil.equals(item.getName(), ".."))
      {
         item = parentDirectory_;
      }
      
      buffer_.setLength(0);
      SelectionCommitEvent.fire(DirectoryContentsWidget.this, item);
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

      List<FileSystemItem> items = dataProvider_.getList();
      for (int i = 0, n = items.size(); i < n; i++)
      {
         FileSystemItem item = items.get(i);
         String fileName = transformer.transform(item.getName());
         if (fileName.startsWith(string))
         {
            setSelectedRow(i);
            return true;
         }
      }

      return false;
   }

   private void selectBufferMatch(char ch)
   {
      buffer_.append(ch);
      
      String buffer = buffer_.toString();
      if (selectBufferMatchImpl(buffer, new IdentityStringTransformer()))
         return;

      if (selectBufferMatchImpl(buffer, new LowerCaseStringTransformer()))
         return;
   }

   public void setSelectedRow(Integer row)
   {
      table_.setKeyboardSelectedRow(row);
      SelectionEvent.fire(DirectoryContentsWidget.this, getSelectedItem());
   }

   public String getSelectedValue()
   {
      FileSystemItem item = getSelectedItem();
      return item == null ? "" : item.getName();
   }

   public FileSystemItem getSelectedItem()
   {
      int row = table_.getKeyboardSelectedRow();
      if (row == -1)
         return null;
      
      FileSystemItem item = dataProvider_.getList().get(row);
      if (StringUtil.equals(item.getName(), ".."))
         item = parentDirectory_;
      
      return item;
      
   }

   public void showProgress(boolean show)
   {
      if (show)
      {
         progressPanel_.showProgress(300);
      }
      else
      {
         progressPanel_.setWidget(scrollPanel_);
         table_.redraw();
      }
   }

   public void clearContents()
   {
      Roles.getListboxRole().removeAriaActivedescendantProperty(table_.getElement());
      dataProvider_.getList().clear();
      scrollPanel_.scrollToTop();
      scrollPanel_.scrollToLeft();
   }

   public void setContents(FileSystemItem[] contents,
                           FileSystemItem parentDirectory)
   {
      clearContents();

      parentDirectory_ = parentDirectory;
      if (parentDirectory != null)
         addItem(FileSystemItem.createDir(".."));

      for (FileSystemItem fsi : contents)
         addItem(fsi);

      showProgress(false);
   }

   private int addItem(FileSystemItem item)
   {
      dataProvider_.getList().add(item);
      return dataProvider_.getList().size() - 1;
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
      int rowNum = addItem(directory);
      TableElement table = (TableElement) table_.getElement().cast();
      TableRowElement row = table.getRows().getItem(rowNum);
      row.scrollIntoView();
      scrollPanel_.setHorizontalScrollPosition(0);
      setSelectedRow(rowNum);
   }

   public void focus()
   {
      Scheduler.get().scheduleDeferred(() ->
      {
         int row = table_.getKeyboardSelectedRow();
         table_.setKeyboardSelectedRow(row);
      });
   }
   
   private final ListDataProvider<FileSystemItem> dataProvider_;
   private final VirtualizedDataGrid<FileSystemItem> table_;
   private final Column<FileSystemItem, FileIcon> iconColumn_;
   private final TextColumn<FileSystemItem> nameColumn_;
   private final TextColumn<FileSystemItem> sizeColumn_;
   private final TextColumn<FileSystemItem> dateColumn_;
   private final ScrollPanelWithClick scrollPanel_;
   private final SimplePanelWithProgress progressPanel_;
   private final FileDialogStyles styles_ = FileDialogResources.INSTANCE.styles();
   private final StringBuilder buffer_;
   private final Timer bufferTimer_;
   
   private FileSystemItem parentDirectory_;
   
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
   private static final int BUFFER_LIFETIME_MS = 700;
}
