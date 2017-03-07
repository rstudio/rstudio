/*
 * FilesList.java
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
package org.rstudio.studio.client.workbench.views.files.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.files.Files;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;

public class FilesList extends Composite
{
   public FilesList(final Files.Display.Observer observer,
                    final FileTypeRegistry fileTypeRegistry)
   {
      observer_ = observer;
      
      // create data provider and sort handler
      dataProvider_ = new ListDataProvider<FileSystemItem>();
      sortHandler_ = new ColumnSortEvent.ListHandler<FileSystemItem>(
                                                      dataProvider_.getList());
      
      // create cell table
      filesDataGrid_ = new DataGrid<FileSystemItem>(
                                          15,
                                          FilesListDataGridResources.INSTANCE,
                                          KEY_PROVIDER);
      selectionModel_ = new MultiSelectionModel<FileSystemItem>(KEY_PROVIDER);
      filesDataGrid_.setSelectionModel(
         selectionModel_, 
         DefaultSelectionEventManager.<FileSystemItem> createCheckboxManager());
      filesDataGrid_.setWidth("100%");
      
      filesDataGrid_.getElement().getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
      
      // hook-up data provider 
      dataProvider_.addDataDisplay(filesDataGrid_);
      
      // add columns
      addSelectionColumn();
      addIconColumn(fileTypeRegistry);
      nameColumn_ = addNameColumn();
      sizeColumn_ = addSizeColumn();
      modifiedColumn_ = addModifiedColumn();
      
      // initialize sorting
      addColumnSortHandler();
      
      // enclose in scroll panel
      layoutPanel_ = new ResizeLayoutPanel();
      initWidget(layoutPanel_);
      layoutPanel_.setWidget(filesDataGrid_);
      
      layoutPanel_.addResizeHandler(new ResizeHandler()
      {
         @Override
         public void onResize(ResizeEvent event)
         {
            FilesList.this.onResize(event.getWidth(), event.getHeight());
         }
      });
   }
   
   private Column<FileSystemItem, Boolean> addSelectionColumn()
   {
      Column<FileSystemItem, Boolean> checkColumn = 
         new Column<FileSystemItem, Boolean>(new CheckboxCell(true, false) {
            @Override
            public void render(Context context, Boolean value, SafeHtmlBuilder sb) 
            {
               // don't render the check box if its for the parent path
               if (parentPath_ == null || context.getIndex() > 0)
                  super.render(context, value, sb);
            }
         }) 
         {
            @Override
            public Boolean getValue(FileSystemItem item)
            {
               return selectionModel_.isSelected(item);
            }
            
            
         };
      checkColumn.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      filesDataGrid_.addColumn(checkColumn); 
      filesDataGrid_.setColumnWidth(checkColumn, CHECK_COLUMN_WIDTH_PIXELS, Unit.PX);
      
      return checkColumn;
   }
  
   
   private Column<FileSystemItem, ImageResource> addIconColumn(
                              final FileTypeRegistry fileTypeRegistry)
   {
      Column<FileSystemItem, ImageResource> iconColumn = 
         new Column<FileSystemItem, ImageResource>(new ImageResourceCell()) {

            @Override
            public ImageResource getValue(FileSystemItem object)
            {
               if (object == parentPath_)
                  return new ImageResource2x(FileIconResources.INSTANCE.iconUpFolder2x());
               else
                  return fileTypeRegistry.getIconForFile(object);
            }
         };
      iconColumn.setSortable(true);
      filesDataGrid_.addColumn(iconColumn, 
                                SafeHtmlUtils.fromSafeConstant("<br/>"));
      filesDataGrid_.setColumnWidth(iconColumn, ICON_COLUMN_WIDTH_PIXELS, Unit.PX);
    
      sortHandler_.setComparator(iconColumn, new FilesListComparator() {
         @Override
         public int doCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            if (arg0.isDirectory() && !arg1.isDirectory())
               return 1;
            else if (arg1.isDirectory() && !arg0.isDirectory())
               return -1;
            else
               return arg0.getExtension().compareTo(arg1.getExtension());
         }
      });
      
      return iconColumn;
   }

   private LinkColumn<FileSystemItem> addNameColumn()
   {
      LinkColumn<FileSystemItem> nameColumn = new LinkColumn<FileSystemItem>(
         dataProvider_, 
         new OperationWithInput<FileSystemItem>() 
         {
            public void execute(FileSystemItem input)
            {
               observer_.onFileNavigation(input);  
            }   
         }) 
         {
            @Override
            public String getValue(FileSystemItem item)
            {
               if (item == parentPath_)
                  return "..";
               else
                  return item.getName();
            }
         };
      nameColumn.setSortable(true);
      filesDataGrid_.addColumn(nameColumn, "Name");
      
      sortHandler_.setComparator(nameColumn, new FilesListComparator() {
         @Override
         public int doCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            return arg0.getName().compareToIgnoreCase(arg1.getName());
         }
      });
      
      return nameColumn;
   }
   
   
   private TextColumn<FileSystemItem>  addSizeColumn()
   {
      TextColumn<FileSystemItem> sizeColumn = new TextColumn<FileSystemItem>() {
         public String getValue(FileSystemItem file)
         {
            if (!file.isDirectory())
               return StringUtil.formatFileSize(file.getLength());
            else
               return new String();
         } 
      };  
      sizeColumn.setSortable(true);
      filesDataGrid_.addColumn(sizeColumn, "Size");
      filesDataGrid_.setColumnWidth(sizeColumn, SIZE_COLUMN_WIDTH_PIXELS, Unit.PX);
      
      sortHandler_.setComparator(sizeColumn, new FoldersOnBottomComparator() {
         @Override
         public int doItemCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            return new Long(arg0.getLength()).compareTo(
                                             new Long(arg1.getLength()));
         }
      });
      
      return sizeColumn;
   }

   
   private TextColumn<FileSystemItem> addModifiedColumn()
   {
      TextColumn<FileSystemItem> modColumn = new TextColumn<FileSystemItem>() {
         public String getValue(FileSystemItem file)
         {
            if (!file.isDirectory())
               return StringUtil.formatDate(file.getLastModified());
            else
               return new String();
         } 
      };  
      modColumn.setSortable(true);
      filesDataGrid_.addColumn(modColumn, "Modified");
      filesDataGrid_.setColumnWidth(modColumn, MODIFIED_COLUMN_WIDTH_PIXELS, Unit.PX); 
      
      sortHandler_.setComparator(modColumn, new FoldersOnBottomComparator() {
         @Override
         public int doItemCompare(FileSystemItem arg0, FileSystemItem arg1)
         {
            return arg0.getLastModified().compareTo(arg1.getLastModified());
         }
      });
      
      return modColumn;
   }
   
   private void addColumnSortHandler()
   {
      filesDataGrid_.addColumnSortHandler(new Handler() {
         @Override
         public void onColumnSort(ColumnSortEvent event)
         {     
            ColumnSortList sortList = event.getColumnSortList();

            // insert the default initial sort order for size and modified
            if (!applyingProgrammaticSort_)
            {
               if (event.getColumn().equals(sizeColumn_) && 
                   forceSizeSortDescending)
               {
                  forceSizeSortDescending = false;
                  forceModifiedSortDescending = true;
                  sortList.insert(0, 
                                  new com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo(event.getColumn(), false));
               }
               else if (event.getColumn().equals(modifiedColumn_) && 
                        forceModifiedSortDescending)
               {
                  forceModifiedSortDescending = false;
                  forceSizeSortDescending = true;
                  sortList.insert(0, 
                                  new com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo(event.getColumn(), false));
               }
               else
               {
                  forceModifiedSortDescending = true;
                  forceSizeSortDescending = true;
               } 
            }
            
            // record sort order and fire event to observer
            JsArray<ColumnSortInfo> sortOrder = newSortOrderArray();
            for (int i=0; i<sortList.size(); i++)
            {
               // match the column index
               com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo sortInfo = sortList.get(i);
               Object column = sortInfo.getColumn();
               
               for (int c=0; c<filesDataGrid_.getColumnCount(); c++)
               {
                  if (filesDataGrid_.getColumn(c).equals(column))
                  { 
                     boolean ascending = sortInfo.isAscending();
                     sortOrder.push(ColumnSortInfo.create(c, ascending));
                     break;
                  }
               }
            }        
            observer_.onColumnSortOrderChanaged(sortOrder);
    
            // record active sort column ascending state
            activeSortColumnAscending_ = event.isSortAscending();
            
            // delegate the sort
            sortHandler_.onColumnSort(event);
         }
         
         private native final JsArray<ColumnSortInfo> newSortOrderArray()
         /*-{
            return [];
         }-*/;       
         private boolean forceSizeSortDescending = true;
         private boolean forceModifiedSortDescending = true;
      });
   }
   
  
  
   public void setColumnSortOrder(JsArray<ColumnSortInfo> sortOrder)
   {
      if (sortOrder != null)
      {
         ColumnSortInfo.setSortList(filesDataGrid_, sortOrder);
      }
      else
      {
         ColumnSortList columnSortList = filesDataGrid_.getColumnSortList();
         columnSortList.clear();
         columnSortList.push(nameColumn_);
      }
   }
   
   
   public void displayFiles(FileSystemItem containingPath, 
                            JsArray<FileSystemItem> files)
   {
      // clear the selection
      selectNone();
      
      // set containing path
      containingPath_ = containingPath;
      parentPath_ = containingPath_.getParentPath();
      
      // set page size (+1 for parent path)
      filesDataGrid_.setPageSize(files.length() + 1);
      
      // get underlying list
      List<FileSystemItem> fileList = dataProvider_.getList();
      fileList.clear();
            
      // add entry for parent path if we have one
      if (parentPath_ != null)
         fileList.add(parentPath_);
      
      // add files to table
      for (int i=0; i<files.length(); i++)
         fileList.add(files.get(i));
           
      // apply sort list
      applyColumnSortList();
      
      // fire selection changed
      observer_.onFileSelectionChanged();
   }
   
   public void selectAll()
   {
      for (FileSystemItem item : dataProvider_.getList())
      {
         if (item != parentPath_)
            selectionModel_.setSelected(item, true);
      }
   }
   
   public void selectNone()
   {
      selectionModel_.clear();
   }
   
   
   public ArrayList<FileSystemItem> getSelectedFiles()
   {    
      // first make sure there are no leftover items in the selected set
      Set<FileSystemItem> selectedSet = selectionModel_.getSelectedSet();
      selectedSet.retainAll(dataProvider_.getList());
   
      return new ArrayList<FileSystemItem>(selectedSet);
   }
   
   public void updateWithAction(FileChange viewAction)
   {        
      final FileSystemItem file = viewAction.getFile();
      final List<FileSystemItem> files = getFiles();
      switch(viewAction.getType())
      {
      case FileChange.ADD:
         if (file.getParentPath().equalTo(containingPath_))
         {
            int row = rowForFile(file);
            if (row == -1)
            {
               files.add(file);
               filesDataGrid_.setPageSize(files.size() + 1);
            }
            else
            {
               // since we eagerly perform renames at the client UI
               // layer then sometimes an "added" file is really just
               // a rename. in this case the file already exists due
               // to the eager rename in the client but still needs its
               // metadata updated
               files.set(row, file);
            }
         }
         break;
         
      case FileChange.MODIFIED:
         {
            int row = rowForFile(file);
            if (row != -1)
               files.set(row, file);
         }
         break;
 
      case FileChange.DELETE:
         {
            int row = rowForFile(file);
            if (row != -1)
            {
               files.remove(row);
               
               // if a file is deleted and then re-added within the same
               // event loop (as occurs when gedit saves a text file) the
               // table doesn't always update correctly (it has a duplicate
               // of the item deleted / re-added). the call to flush overcomes
               // this issue
               dataProvider_.flush();
            }
         }
         break;
      
      default:
         Debug.log("Unexpected file change type: " + viewAction.getType());
         
         break;
      }
   }
   
   public void renameFile(FileSystemItem from, FileSystemItem to)
   {
      int index = getFiles().indexOf(from);
      if (index != -1)
      {
         selectNone();
         getFiles().set(index, to);
      }
   }
   
   private List<FileSystemItem> getFiles()
   {
      return dataProvider_.getList();
   }
   
   private int rowForFile(FileSystemItem file)
   {
      List<FileSystemItem> files = getFiles();
      for (int i=0; i<files.size(); i++)
         if (files.get(i).equalTo(file))
            return i ;
      
      return -1;
   }
   
   private void applyColumnSortList()
   {
      applyingProgrammaticSort_ = true;
      ColumnSortEvent.fire(filesDataGrid_, 
                           filesDataGrid_.getColumnSortList());
      applyingProgrammaticSort_ = false;
   }
   
   public void redraw()
   {
      onResize();
      
      // deferred to ensure that browser has responded to our
      // resize request
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            filesDataGrid_.redraw();
         }
      });
   }
   
   public void onResize()
   {
      onResize(layoutPanel_.getOffsetWidth(), layoutPanel_.getOffsetHeight());
   }
   
   private void onResize(int width, int height)
   {
      // Enforce a minimum column width on the name column.
      int newState = width < BOUNDARY_WIDTH_PIXELS ? STATE_SMALL : STATE_LARGE;

      // Avoid over-eager updating of column widths.
      if (state_ == STATE_LARGE && state_ == newState)
         return;

      state_ = newState;
      if (state_ == STATE_LARGE)
      {
         filesDataGrid_.setColumnWidth(nameColumn_, "auto");
         filesDataGrid_.setColumnWidth(sizeColumn_, SIZE_COLUMN_WIDTH_PIXELS, Unit.PX);
         filesDataGrid_.setColumnWidth(modifiedColumn_, MODIFIED_COLUMN_WIDTH_PIXELS, Unit.PX);
         return;
      }

      // Otherwise, we need to update column widths one by one.
      // The right-most columns lose width first.
      // TODO: Properly abstract this out into some kind of sizing
      // policy that DataGrids can adopt / use.
      int leftoverWidth = width
            - MINIMUM_NAME_COLUMN_WIDTH_PIXELS
            - CHECK_COLUMN_WIDTH_PIXELS
            - ICON_COLUMN_WIDTH_PIXELS;

      if (leftoverWidth < 0)
      {
         filesDataGrid_.setColumnWidth(sizeColumn_, 0, Unit.PX);
         filesDataGrid_.setColumnWidth(modifiedColumn_, 0, Unit.PX);

         // Adjust the name column width
         int nameWidth = width - CHECK_COLUMN_WIDTH_PIXELS - ICON_COLUMN_WIDTH_PIXELS;
         filesDataGrid_.setColumnWidth(nameColumn_, nameWidth < 0 ? 0 : nameWidth, Unit.PX);
      }
      else if (leftoverWidth < SIZE_COLUMN_WIDTH_PIXELS)
      {
         filesDataGrid_.setColumnWidth(sizeColumn_, leftoverWidth, Unit.PX);
         filesDataGrid_.setColumnWidth(modifiedColumn_, 0, Unit.PX);
         filesDataGrid_.setColumnWidth(nameColumn_, MINIMUM_NAME_COLUMN_WIDTH_PIXELS, Unit.PX);
      }
      else if (leftoverWidth < SIZE_COLUMN_WIDTH_PIXELS + MODIFIED_COLUMN_WIDTH_PIXELS)
      {
         filesDataGrid_.setColumnWidth(sizeColumn_, SIZE_COLUMN_WIDTH_PIXELS, Unit.PX);
         filesDataGrid_.setColumnWidth(modifiedColumn_, leftoverWidth - SIZE_COLUMN_WIDTH_PIXELS, Unit.PX);
         filesDataGrid_.setColumnWidth(nameColumn_, MINIMUM_NAME_COLUMN_WIDTH_PIXELS, Unit.PX);
      }

   }
   
   private static final ProvidesKey<FileSystemItem> KEY_PROVIDER = 
      new ProvidesKey<FileSystemItem>() {
         @Override
         public Object getKey(FileSystemItem item)
         {
            return item.getPath();
         }
    };
    
    // comparator which ensures that the parent path is always on top
    private abstract class FilesListComparator implements Comparator<FileSystemItem>
    {     
       @Override
       public int compare(FileSystemItem arg0, FileSystemItem arg1)
       {
          int ascendingFactor = activeSortColumnAscending_ ? -1 : 1;
          
          if (arg0 == parentPath_)
             return 1 * ascendingFactor;
          else if (arg1 == parentPath_)
             return -1 * ascendingFactor;
          else
             return doCompare(arg0, arg1);
       }
       
       protected abstract int doCompare(FileSystemItem arg0, FileSystemItem arg1);    
    }
    
    private abstract class SeparateFoldersComparator extends FilesListComparator
    {
       public SeparateFoldersComparator(boolean foldersOnBottom)
       {
          if (foldersOnBottom)
             sortFactor_ = 1;
          else
             sortFactor_ = -1;
       }
       
       protected int doCompare(FileSystemItem arg0, FileSystemItem arg1)
       {
          int ascendingResult = activeSortColumnAscending_ ? 1 : -1;
          
          if (arg0.isDirectory() && !arg1.isDirectory())
             return ascendingResult * sortFactor_;
          else if (arg1.isDirectory() && !arg0.isDirectory())
             return -ascendingResult * sortFactor_;
          else
             return doItemCompare(arg0, arg1);
       }
       
       protected abstract int doItemCompare(FileSystemItem arg0, FileSystemItem arg1);    
       
       private final int sortFactor_ ;   
    }
    
    private abstract class FoldersOnBottomComparator extends SeparateFoldersComparator
    {
       public FoldersOnBottomComparator() 
       { 
          super(true); 
       }
    }
    
    @SuppressWarnings("unused")
    private abstract class FoldersOnTopComparator extends SeparateFoldersComparator
    {
       public FoldersOnTopComparator() 
       { 
          super(false); 
       }
    }
    
   
   private FileSystemItem containingPath_ = null;
   private FileSystemItem parentPath_ = null;
  
   private final DataGrid<FileSystemItem> filesDataGrid_; 
   private final LinkColumn<FileSystemItem> nameColumn_;
   private final TextColumn<FileSystemItem> sizeColumn_;
   private final TextColumn<FileSystemItem> modifiedColumn_;
   private boolean activeSortColumnAscending_ = true;
   private boolean applyingProgrammaticSort_ = false;
   
   
   private final MultiSelectionModel<FileSystemItem> selectionModel_;
   private final ListDataProvider<FileSystemItem> dataProvider_;
   private final ColumnSortEvent.ListHandler<FileSystemItem> sortHandler_;

   private final Files.Display.Observer observer_ ;
   private final ResizeLayoutPanel layoutPanel_ ;  
   
   private static final int CHECK_COLUMN_WIDTH_PIXELS = 30;
   private static final int ICON_COLUMN_WIDTH_PIXELS = 26;
   private static final int SIZE_COLUMN_WIDTH_PIXELS = 80;
   private static final int MODIFIED_COLUMN_WIDTH_PIXELS = 160;
   
   private static final int BOUNDARY_WIDTH_PIXELS = 500;
   private static final int MINIMUM_NAME_COLUMN_WIDTH_PIXELS = 
         BOUNDARY_WIDTH_PIXELS -
         CHECK_COLUMN_WIDTH_PIXELS -
         ICON_COLUMN_WIDTH_PIXELS -
         SIZE_COLUMN_WIDTH_PIXELS -
         MODIFIED_COLUMN_WIDTH_PIXELS;
   
   private static final int STATE_UNKNOWN = 0;
   private static final int STATE_SMALL   = 1;
   private static final int STATE_LARGE   = 2;

   private int state_ = STATE_UNKNOWN;
   
}
