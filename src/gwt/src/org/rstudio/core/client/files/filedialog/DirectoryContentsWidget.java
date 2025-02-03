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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.RowTable;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;

import java.util.ArrayList;
import java.util.List;

public class DirectoryContentsWidget extends Composite
   implements HasSelectionHandlers<FileSystemItem>,
              HasSelectionCommitHandlers<FileSystemItem>,
              CanFocus
{
   public DirectoryContentsWidget(FileSystemContext context)
   {
      data_ = new ArrayList<>();
      table_ = new RowTable<FileSystemItem>(constants_.directoryContentsLabel())
      {
         private final FileTypeRegistry registry_ = RStudioGinjector.INSTANCE.getFileTypeRegistry();
         
         @Override
         public double getRowHeight()
         {
            return 24;
         }
         
         @Override
         public int[] getColumnWidths()
         {
            return new int[] { 22, 222, 70, 150, 20 };
         }
         
         @Override
         public String getKey(FileSystemItem object)
         {
            return object.getName();
         }
         
         @Override
         public void drawRowImpl(FileSystemItem object, TableRowElement rowEl)
         {
            // Reuse some variable names
            TableCellElement cellEl;
            DivElement divEl;
            
            // Icon
            cellEl = Document.get().createTDElement();
            FileIcon icon = registry_.getIconForFile(object);
            cellEl.appendChild(icon.getImage().getElement());
            rowEl.appendChild(cellEl);
            
            // File name
            cellEl = Document.get().createTDElement();
            divEl = Document.get().createDivElement();
            divEl.setInnerText(object.getName());
            cellEl.appendChild(divEl);
            rowEl.appendChild(cellEl);
            
            if (object.isDirectory())
            {
               cellEl.setAttribute("colspan", "4");
            }
            else
            {
               // File size
               cellEl = Document.get().createTDElement();
               divEl = Document.get().createDivElement();
               divEl.setInnerText(StringUtil.formatFileSize(object.getLength()));
               cellEl.appendChild(divEl);
               rowEl.appendChild(cellEl);

               // File date
               cellEl = Document.get().createTDElement();
               divEl = Document.get().createDivElement();
               divEl.setInnerText(StringUtil.formatDate(object.getLastModified()));
               cellEl.appendChild(divEl);
               rowEl.appendChild(cellEl);

               // Padding (for scrollbar)
               cellEl = Document.get().createTDElement();
               rowEl.appendChild(cellEl);
            }
         }

      };
      
      table_.getElement().getStyle().setBackgroundColor("white");
      table_.getElement().setTabIndex(0);
      table_.setSize("500px", "300px");
      
      table_.addDomHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_BACKSPACE)
            {
               if (parentDirectory_ != null)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  commitSelection(parentDirectory_);
               }
            }
         }
      }, KeyDownEvent.getType());
      
      table_.addSelectionHandler(new SelectionHandler<FileSystemItem>()
      {
         @Override
         public void onSelection(SelectionEvent<FileSystemItem> event)
         {
            commitSelection(event.getSelectedItem());
         }
      });
      
      // presented to screen readers as a single-select listbox
      Roles.getListboxRole().set(table_.getElement());
      Roles.getListboxRole().setAriaLabelProperty(table_.getElement(), constants_.directoryContentsLabel());

      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setInheritEditorTheme(false);
      progressPanel_.setWidget(null);
      progressPanel_.setHeight("300px");
      initWidget(progressPanel_);
   }

   private void commitSelection(FileSystemItem item)
   {
      if (StringUtil.equals(item.getName(), ".."))
      {
         item = parentDirectory_;
      }
      
      SelectionCommitEvent.fire(DirectoryContentsWidget.this, item);
   }

   public void setSelectedRow(Integer row)
   {
      table_.selectRow(row);
      SelectionEvent.fire(DirectoryContentsWidget.this, getSelectedItem());
   }

   public String getSelectedValue()
   {
      FileSystemItem item = getSelectedItem();
      return item == null ? "" : item.getName();
   }

   public FileSystemItem getSelectedItem()
   {
      return table_.getSelectedItem();
   }

   // This API is a bit oddly named. It's called with 'true' to indicate
   // that we're almost ready to show content, and later with 'false'
   // after the content is ready to be shown.
   public void showProgress(boolean show)
   {
      if (show)
      {
         progressPanel_.showProgress(300);
      }
      else
      {
         table_.draw(data_);
         progressPanel_.setWidget(table_);
      }
   }

   public void clearContents()
   {
      Roles.getListboxRole().removeAriaActivedescendantProperty(table_.getElement());
      table_.clear();
      table_.scrollToTop();
      table_.scrollToLeft();
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

   private void addItem(FileSystemItem item)
   {
      data_.add(item);
   }

   public Point getScrollPosition()
   {
      return Point.create(
            table_.getHorizontalScrollPosition(),
            table_.getVerticalScrollPosition());
   }

   public void setScrollPosition(Point p)
   {
      table_.setVerticalScrollPosition(p.getY());
      table_.setHorizontalScrollPosition(p.getX());
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
      addItem(directory);
      int rowNum = data_.size() - 1;
      TableElement table = (TableElement) table_.getElement().cast();
      TableRowElement row = table.getRows().getItem(rowNum);
      row.scrollIntoView();
      table_.setHorizontalScrollPosition(0);
      setSelectedRow(rowNum);
   }

   public void focus()
   {
      table_.getElement().focus();
   }
   
   
   private final List<FileSystemItem> data_;
   private final RowTable<FileSystemItem> table_;
   private final SimplePanelWithProgress progressPanel_;
   
   private FileSystemItem parentDirectory_;
   
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
