/*
 * SavePlotAsTargetEditor.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import java.util.HashMap;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsImageFormat;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class SavePlotAsTargetEditor extends Composite implements CanFocus
{
   public SavePlotAsTargetEditor(String defaultFormat,
                                 SavePlotAsImageContext context)
   {
      context_ = context;
      
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();

      Grid grid = new Grid(3, 2);
      grid.setCellPadding(0);
    
      Label imageFormatLabel = new Label("Image format:");
      imageFormatLabel.setStylePrimaryName(styles.exportTargetLabel());
          
      grid.setWidget(0, 0, imageFormatLabel);
      imageFormatListBox_ = new ListBox();
      JsArray<SavePlotAsImageFormat> formats = context.getFormats();
      int selectedIndex = 0;
      for (int i=0; i<formats.length(); i++)
      {
         SavePlotAsImageFormat format = formats.get(i);
         if (format.getExtension().equals(defaultFormat))
            selectedIndex = i;
         imageFormatListBox_.addItem(format.getName(), format.getExtension());
      }
      imageFormatListBox_.setSelectedIndex(selectedIndex);
      imageFormatListBox_.setStylePrimaryName(styles.imageFormatListBox());
      grid.setWidget(0, 1, imageFormatListBox_);
           
      ThemedButton directoryButton = new ThemedButton("Directory...");
      directoryButton.setStylePrimaryName(styles.directoryButton());
      directoryButton.getElement().getStyle().setMarginLeft(-2, Unit.PX);
      grid.setWidget(1, 0, directoryButton);
      directoryButton.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            fileDialogs_.chooseFolder(
               "Choose Directory",
               fileSystemContext_,
               FileSystemItem.createDir(directoryLabel_.getTitle().trim()),
               new ProgressOperationWithInput<FileSystemItem>() {

                 public void execute(FileSystemItem input,
                                     ProgressIndicator indicator)
                 {
                    if (input == null)
                       return;
                    
                    indicator.onCompleted();
                    
                    // update default
                    ExportPlot.setDefaultSaveDirectory(context_.getDirectory(),
                                                       input);
                    
                    // set display
                    setDirectory(input);  
                 }          
               });
         }
      });
      
      directoryLabel_ = new Label();
      setDirectory(context_.getDirectory());
      directoryLabel_.setStylePrimaryName(styles.directoryLabel());
      grid.setWidget(1, 1, directoryLabel_);
      
      Label fileNameLabel = new Label("File name:");
      fileNameLabel.setStylePrimaryName(styles.fileNameLabel());
      grid.setWidget(2, 0, fileNameLabel);
      fileNameTextBox_ = new TextBox();
      fileNameTextBox_.setText(context.getUniqueFileStem());
      fileNameTextBox_.setStylePrimaryName(styles.fileNameTextBox());
      grid.setWidget(2, 1, fileNameTextBox_);
        
      initWidget(grid);
   }
   
   public void focus()
   {
      fileNameTextBox_.setFocus(true);
      fileNameTextBox_.selectAll();
   }

   public String getFormat()
   {
      return imageFormatListBox_.getValue(
                                 imageFormatListBox_.getSelectedIndex());
   }
    
   public FileSystemItem getTargetPath()
   {
      // first determine format extension
      String ext = "." + imageFormatListBox_.getValue(
                                       imageFormatListBox_.getSelectedIndex());
      
      return ExportPlot.composeTargetPath(ext, fileNameTextBox_, directory_);  
   }
   
   public FileSystemItem getTargetDirectory()
   {
      return directory_;
   }
  
   
   private void setDirectory(FileSystemItem directory)
   {
      // set directory
      directory_ = directory;
        
      // set label
      String dirLabel = ExportPlot.shortDirectoryName(directory, 250);
      directoryLabel_.setText(dirLabel);
      
      // set tooltip
      directoryLabel_.setTitle(directory.getPath());
   }
   
   
   private ListBox imageFormatListBox_;
   private TextBox fileNameTextBox_;
   private FileSystemItem directory_;
   private Label directoryLabel_;
  
   
   private final SavePlotAsImageContext context_;
 
   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();
   
   private final FileDialogs fileDialogs_ = 
      RStudioGinjector.INSTANCE.getFileDialogs();
   
   // remember what directory was chosen for plot export for various
   // working directories
   static HashMap<String, FileSystemItem> initialDirectories_ = 
                                       new HashMap<String,FileSystemItem>();
   
   
   
 
   
}
