/*
 * FileList.java
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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.filedialog.FileDialogResources;
import org.rstudio.core.client.files.filedialog.FileDialogStyles;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.files.Files;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;

import java.util.ArrayList;

// NOTE: this widget derives its look and feel from the DirectoryContentsWidget.
// We ultimately should merge the implementations of these widgets

public class FileList extends Composite
{
   public FileList(Files.Display.Observer observer,
                   FileTypeRegistry fileTypeRegistry)
   {   
      observer_ = observer;
      fileTypeRegistry_ = fileTypeRegistry;
      scrollPanel_ = new ScrollPanel();
      initWidget(scrollPanel_);
      setStylePrimaryName(ThemeStyles.INSTANCE.fileList());
   }
   
   @Override 
   protected void onUnload()
   {
      clearFiles();
      super.onUnload();
   }
   
   public void clearFiles()
   {
      containingPath_ = null;
      files_.clear();
      if (filesTable_ != null)
         filesTable_.removeFromParent();
      scrollPanel_.setWidget(null);
   }
   
   public void displayFiles(FileSystemItem containingPath, 
                            JsArray<FileSystemItem> files)
   {
      // reset state
      clearFiles();
      
      // set containing path
      containingPath_ = containingPath;
          
      // create flex table 
      filesTable_ = new FlexTable();
      filesTable_.setCellSpacing(0);
      filesTable_.setCellPadding(2);
      filesTable_.setWidth("100%");
      ColumnFormatter columnFormatter = filesTable_.getColumnFormatter();
      columnFormatter.setWidth(COL_CHECKBOX, "20px");
      
      // if there is a navigable parent then create a special parent entry
      FileSystemItem parentPath = containingPath.getParentPath();
      if (parentPath != null)
         addFile(parentPath, true);
              
      // add files to table
      for (int i=0; i<files.length(); i++)
         addFile(files.get(i));
         
      // show table in scroll panel
      scrollPanel_.setWidget(filesTable_);
      
      // fire selection changed
      observer_.onFileSelectionChanged();
   }
   
   public void selectAll()
   {
      setAllSelections(true);
   }
   
   public void selectNone()
   {
      setAllSelections(false);
   }
   
   
   public ArrayList<FileSystemItem> getSelectedFiles()
   {
      ArrayList<FileSystemItem> selectedFiles = new ArrayList<FileSystemItem>() ;
      for (int i = itemStartIndex(); i<filesTable_.getRowCount();i++)
      {
         CheckBox checkBox = (CheckBox) filesTable_.getWidget(i, 0);
         if (checkBox.getValue())
            selectedFiles.add(files_.get(i));
      }
      
      return selectedFiles ;
   }
   
   
  
   public void updateWithAction(FileChange viewAction)
   {   
      FileSystemItem file = viewAction.getFile();
      switch(viewAction.getType())
      {
      case FileChange.ADD:
         if (file.getParentPath().equalTo(containingPath_))
         {
            int row = rowForFile(file);
            if (row == -1)
            {
               addFile(file);
               scrollToBottom();
            }
            else
            {
               // since we eagerly perform renames at the client UI
               // layer then sometimes an "added" file is really just
               // a rename. in this case the file already exists due
               // to the eager rename in the client but still needs its
               // metadata updated
               files_.set(row, file);
               setFileWidgets(row, file);
            }
         }
         break;
         
      case FileChange.MODIFIED:
         {
            int row = rowForFile(file);
            if (row != -1)
            {
               files_.set(row, file);
               setFileWidgets(row, file);
            }
         }
         break;
 
      case FileChange.DELETE:
         {
            int row = rowForFile(file);
            if (row != -1)
            {
               files_.remove(row);
               filesTable_.removeRow(row);
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
      int row = rowForFile(from);
      if (row != -1)
      {
         files_.set(row, to);
         setFileWidgets(row, to);
      }
   }
   
   public void scrollToBottom()
   {
      scrollPanel_.scrollToBottom();
   }
   
   private void addFile(FileSystemItem file)
   {
      addFile(file, false);
   }
         
   private void addFile(FileSystemItem file, boolean isParentItem)
   {
      files_.add(file);
      int row = files_.size()-1;
      setFileWidgets(row, file, isParentItem);
   }
   
   private void setFileWidgets(int row, final FileSystemItem file)
   {
      setFileWidgets(row, file, false);
   }
   
   private void setFileWidgets(int row, 
                               final FileSystemItem file,
                               boolean isParentItem)
   {  
      CellFormatter cellFmt = filesTable_.getCellFormatter();
      
      // check box (create only, don't overwrite existing)
      if (row >= filesTable_.getRowCount())
      {
         if (!isParentItem)
         {
            CheckBox checkBox = new CheckBox();
            checkBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                  public void onValueChange(ValueChangeEvent<Boolean> event) {
                     observer_.onFileSelectionChanged();
                  }                           
            });
            filesTable_.setWidget(row, COL_CHECKBOX, new CheckBox());  
         }
      }
      
      // establish click handler
      ClickHandler clickHandler = new ClickHandler() {
         public void onClick(ClickEvent event) {
            observer_.onFileNavigation(file);
         }};
         
      // customizations for isParnetItem
      int iconCol ;
      Image icon ;
      String name;
      String title = "" ;
      if (isParentItem)
      {
         final String GO_TO_PARENT = "Go to parent directory";
         iconCol = COL_CHECKBOX;
         icon = new Image(FileIconResources.INSTANCE.iconUpFolder());
         icon.setStylePrimaryName(ThemeStyles.INSTANCE.parentDirIcon());
         icon.setTitle(GO_TO_PARENT);
         icon.addClickHandler(clickHandler);
         name = "..";
         title = GO_TO_PARENT;
      }
      else
      {
         iconCol = COL_ICON;
         icon = iconForFile(file);
         name = file.getName();
      }
      filesTable_.setWidget(row, iconCol, icon);
      cellFmt.setStylePrimaryName(row, iconCol, styles_.columnIcon());
      
      // link label
      HyperlinkLabel fileLabel = new HyperlinkLabel(name, clickHandler); 
      fileLabel.setTitle(title);
      filesTable_.setWidget(row, COL_NAME, fileLabel);  
      cellFmt.setStylePrimaryName(row, COL_NAME, styles_.columnName());
      
      if (!file.isDirectory())
      {
         // size
         filesTable_.setText(row, 
                             COL_SIZE, 
                             StringUtil.formatFileSize(file.getLength()));
         cellFmt.setStylePrimaryName(row, COL_SIZE, styles_.columnSize());
      
         // timestamp
         filesTable_.setText(row, 
                             COL_TIMESTAMP, 
                             StringUtil.formatDate(file.getLastModified()));
         cellFmt.setStylePrimaryName(row, COL_TIMESTAMP,styles_.columnDate());
      }
      else
      {
         ((FlexTable.FlexCellFormatter)filesTable_.getCellFormatter()).setColSpan(
                  row, COL_NAME, 3);
      }
   }
   
   private int rowForFile(FileSystemItem file)
   {
      for (int i=0; i<files_.size(); i++)
         if (files_.get(i).equalTo(file))
            return i ;
      
      return -1;
   }
   
   private Image iconForFile(FileSystemItem file)
   {
      ImageResource img = fileTypeRegistry_.getIconForFile(file);
      if (img == null)
         img = file.getIcon(); 
      return new Image(img);
   }
   
   private void setAllSelections(boolean checked)
   {
      for (int i=itemStartIndex(); i<filesTable_.getRowCount();i++)
      {
         CheckBox checkBox = (CheckBox) filesTable_.getWidget(i, 0);
         checkBox.setValue(Boolean.valueOf(checked));
      }
   }
   
   private int itemStartIndex()
   {
      // (skip the first row if we have a parent)
      return containingPath_.getParentPath() != null ? 1 : 0;
   }
   
   private final Files.Display.Observer observer_ ;
   private ScrollPanel scrollPanel_ ;
   private FlexTable filesTable_;
   private FileSystemItem containingPath_ = null;
   private ArrayList<FileSystemItem> files_ = new ArrayList<FileSystemItem>();
   
   private static final int COL_CHECKBOX = 0;
   private static final int COL_ICON = 1;
   private static final int COL_NAME = 2;
   private static final int COL_SIZE = 3;
   private static final int COL_TIMESTAMP = 4;
   private final FileDialogStyles styles_ = FileDialogResources.INSTANCE.styles();
   private FileTypeRegistry fileTypeRegistry_;
}
