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
import org.rstudio.core.client.widget.HyperlinkLabel;
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
         new Column<FileSystemItem, Boolean>(new CheckboxCell(true, false)) 
         {
            @Override
            public Boolean getValue(FileSystemItem item)
            {
               return selectionModel_.isSelected(item);
            }
         };
      checkColumn.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      filesCellTable_.addColumn(checkColumn); 
      
      // file icon
      Column<FileSystemItem, ImageResource> iconColumn = 
         new Column<FileSystemItem, ImageResource>(new ImageResourceCell()) {

            @Override
            public ImageResource getValue(FileSystemItem object)
            {
               return fileTypeRegistry.getIconForFile(object);
            }
         
      };
      filesCellTable_.addColumn(iconColumn);
    
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
               return item.getName();
            }
         };
      filesCellTable_.addColumn(nameColumn);
      
      
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
      filesCellTable_.addColumn(sizeColumn);
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
      filesCellTable_.addColumn(modColumn);
      filesCellTable_.setColumnWidth(modColumn, 160, Unit.PX);
 
      // hookup data provider
      dataProvider_.addDataDisplay(filesCellTable_);
      
      // create vertical pane which encloses the parent widget
      // and the files table
      VerticalPanel verticalPanel = new VerticalPanel();
      goToParentWidget_ = new GoToParentWidget();
      goToParentWidget_.setVisible(false);
      verticalPanel.add(goToParentWidget_);
      verticalPanel.add(filesCellTable_);
      
      // enclose in scroll panel
      scrollPanel_ = new ScrollPanel();
      initWidget(scrollPanel_);
      scrollPanel_.setWidget(verticalPanel);   
   }
   
   public void clearFiles()
   {
      containingPath_ = null;
      goToParentWidget_.setVisible(false);
      dataProvider_.getList().clear();
   }
   
   
   public void displayFiles(FileSystemItem containingPath, 
                            JsArray<FileSystemItem> files)
   {
      // clear
      clearFiles();
      
      // set containing path
      containingPath_ = containingPath;
      
      // if there is a navigable parent then make the parent widget visible
      FileSystemItem parentPath = containingPath.getParentPath();
      if (parentPath != null)
         goToParentWidget_.setVisible(true);
      
      filesCellTable_.setPageSize(files.length());
      
      // get underlying list
      List<FileSystemItem> fileList = dataProvider_.getList();
              
      // add files to table
      for (int i=0; i<files.length(); i++)
         fileList.add(files.get(i));
           
      // fire selection changed
      observer_.onFileSelectionChanged();
   }
   
   public void selectAll()
   {
      for (FileSystemItem item : dataProvider_.getList())
         selectionModel_.setSelected(item, true);
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
               filesCellTable_.setPageSize(files.size());
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                  @Override
                  public void execute()
                  {
                     scrollToBottom();  
                  } 
               });
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
   
   public void scrollToBottom()
   {
      scrollPanel_.scrollToBottom();
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
   
   private static final ProvidesKey<FileSystemItem> KEY_PROVIDER = 
      new ProvidesKey<FileSystemItem>() {
         @Override
         public Object getKey(FileSystemItem item)
         {
            return item.getPath();
         }
    };
    
   private class GoToParentWidget extends Composite
   {
      public GoToParentWidget()
      {
         HorizontalPanel panel = new HorizontalPanel();
         panel.setSpacing(3);
         Image img = new Image(FileIconResources.INSTANCE.iconUpFolder()); 
         panel.add(img);
     
         panel.setCellWidth(img, "22px");
         HyperlinkLabel upLabel = new HyperlinkLabel(
            "..", 
            new ClickHandler() 
            {
               public void onClick(ClickEvent event)
               {
                  FileSystemItem parentPath = containingPath_.getParentPath();
                  if (parentPath != null)
                     observer_.onFileNavigation(parentPath);               
               }   
            });
         upLabel.setClearUnderlineOnClick(true);
         upLabel.setTitle("Go to parent directory");
         panel.add(upLabel);    
         initWidget(panel);
      }
   }
   
   private FileSystemItem containingPath_ = null;
   
   private final GoToParentWidget goToParentWidget_ ;
   private final CellTable<FileSystemItem> filesCellTable_; 
   
   private final MultiSelectionModel<FileSystemItem> selectionModel_;
   private final ListDataProvider<FileSystemItem> dataProvider_;

   private final Files.Display.Observer observer_ ;
   private final ScrollPanel scrollPanel_ ;  
   
 
   
}
