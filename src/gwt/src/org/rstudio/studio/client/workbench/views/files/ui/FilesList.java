/*
 * FilesList.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.files.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.files.Files;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
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
      
      dataProvider_ = new ListDataProvider<FileSystemItem>();
      
      filesCellTable_ = new CellTable<FileSystemItem>(
                                          15,
                                          FilesListCellTableResources.INSTANCE,
                                          KEY_PROVIDER);
     
      selectionModel_ = new MultiSelectionModel<FileSystemItem>(KEY_PROVIDER);
      filesCellTable_.setSelectionModel(
         selectionModel_, 
         DefaultSelectionEventManager.<FileSystemItem> createCheckboxManager());
      filesCellTable_.setWidth("100%", false);
      
      // selection checkbox
      Column<FileSystemItem, Boolean> checkColumn = 
         new Column<FileSystemItem, Boolean>(new CheckboxCell(true, false) {
            @Override
            public void render(Context context, Boolean value, SafeHtmlBuilder sb) 
            {
               // don't render the check box if its for the parent path
               if (getParentPath() == null || context.getIndex() > 0)
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
      filesCellTable_.addColumn(checkColumn); 
      filesCellTable_.setColumnWidth(checkColumn, 20, Unit.PX);
      
      // file icon
      Column<FileSystemItem, ImageResource> iconColumn = 
         new Column<FileSystemItem, ImageResource>(new ImageResourceCell()) {

            @Override
            public ImageResource getValue(FileSystemItem object)
            {
               if (object.equalTo(getParentPath()))
                  return FileIconResources.INSTANCE.iconUpFolder();
               else
                  return fileTypeRegistry.getIconForFile(object);
            }
         
      };
      filesCellTable_.addColumn(iconColumn, 
                                SafeHtmlUtils.fromSafeConstant("<br/>"));
      filesCellTable_.setColumnWidth(iconColumn, 20, Unit.PX);
    
      // file name
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
               if (item.equalTo(getParentPath()))
                  return "..";
               else
                  return item.getName();
            }
         };
      filesCellTable_.addColumn(nameColumn, "Name");
      
      // file size
      TextColumn<FileSystemItem> sizeColumn = new TextColumn<FileSystemItem>() {
         public String getValue(FileSystemItem file)
         {
            if (!file.isDirectory())
               return StringUtil.formatFileSize(file.getLength());
            else
               return new String();
         } 
      };  
      filesCellTable_.addColumn(sizeColumn, "Size");
      filesCellTable_.setColumnWidth(sizeColumn, 80, Unit.PX);
      
      // last modified
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
      filesCellTable_.addColumn(modColumn, "Modified");
      filesCellTable_.setColumnWidth(modColumn, 160, Unit.PX);
 
      // hookup data provider
      dataProvider_.addDataDisplay(filesCellTable_);
      
      // enclose in scroll panel
      scrollPanel_ = new ScrollPanel();
      initWidget(scrollPanel_);
      scrollPanel_.setWidget(filesCellTable_);   
   }
   
   public void clearFiles()
   {
      containingPath_ = null;
      dataProvider_.getList().clear();
   }
   
   
   public void displayFiles(FileSystemItem containingPath, 
                            JsArray<FileSystemItem> files)
   {
      // clear
      clearFiles();
      
      // set containing path
      containingPath_ = containingPath;
      
      // set page size (+1 for parent path)
      filesCellTable_.setPageSize(files.length() + 1);
      
      // get underlying list
      List<FileSystemItem> fileList = dataProvider_.getList();
            
      // add entry for parent path if we have one
      FileSystemItem parentPath = getParentPath();
      if (parentPath != null)
         fileList.add(parentPath);
      
      // add files to table
      for (int i=0; i<files.length(); i++)
         fileList.add(files.get(i));
           
      // fire selection changed
      observer_.onFileSelectionChanged();
   }
   
   public void selectAll()
   {
      for (FileSystemItem item : dataProvider_.getList())
      {
         if (!item.equalTo(getParentPath()))
            selectionModel_.setSelected(item, true);
      }
   }
   
   public void selectNone()
   {
      selectionModel_.clear();
   }
   
   
   public ArrayList<FileSystemItem> getSelectedFiles()
   {    
      return new ArrayList<FileSystemItem>(selectionModel_.getSelectedSet());
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
               filesCellTable_.setPageSize(files.size() + 1);
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
         getFiles().set(index, to);
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
   
   private FileSystemItem getParentPath()
   {
      if (containingPath_ != null)
         return containingPath_.getParentPath();
      else
         return null;
   }
   
   private static final ProvidesKey<FileSystemItem> KEY_PROVIDER = 
      new ProvidesKey<FileSystemItem>() {
         @Override
         public Object getKey(FileSystemItem item)
         {
            return item.getPath();
         }
    };
    
   
   private FileSystemItem containingPath_ = null;
   
   private final CellTable<FileSystemItem> filesCellTable_; 
   
   private final MultiSelectionModel<FileSystemItem> selectionModel_;
   private final ListDataProvider<FileSystemItem> dataProvider_;

   private final Files.Display.Observer observer_ ;
   private final ScrollPanel scrollPanel_ ;  
   
 
   
}
