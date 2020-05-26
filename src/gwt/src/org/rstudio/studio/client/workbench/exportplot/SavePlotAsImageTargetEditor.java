/*
 * SavePlotAsImageTargetEditor.java
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
package org.rstudio.studio.client.workbench.exportplot;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageFormat;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class SavePlotAsImageTargetEditor extends Composite implements CanFocus
{
   public SavePlotAsImageTargetEditor(String defaultFormat,
                                 SavePlotAsImageContext context)
   {
      context_ = context;
      
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();

      LayoutGrid grid = new LayoutGrid(3, 2);
      grid.setCellPadding(0);
    
      imageFormatListBox_ = new ListBox();
      FormLabel imageFormatLabel = new FormLabel("Image format:", imageFormatListBox_);
      imageFormatLabel.setStylePrimaryName(styles.exportTargetLabel());
          
      grid.setWidget(0, 0, imageFormatLabel);
      JsArray<SavePlotAsImageFormat> formats = context.getFormats();
      int selectedIndex = 0;
      for (int i=0; i<formats.length(); i++)
      {
         SavePlotAsImageFormat format = formats.get(i);
         if (format.getExtension() == defaultFormat)
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
               FileSystemItem.createDir(directoryTextBox_.getText().trim()),
               new ProgressOperationWithInput<FileSystemItem>() {

                 public void execute(FileSystemItem input,
                                     ProgressIndicator indicator)
                 {
                    if (input == null)
                       return;
                    
                    indicator.onCompleted();
                    
                    // update default
                    ExportPlotUtils.setDefaultSaveDirectory(input);
                    
                    // set display
                    setDirectory(input);  
                 }          
               });
         }
      });
      
      directoryTextBox_ = new TextBox();
      directoryTextBox_.setReadOnly(true);
      Roles.getTextboxRole().setAriaLabelProperty(directoryTextBox_.getElement(), "Selected Directory");

      setDirectory(context_.getDirectory());
      directoryTextBox_.setStylePrimaryName(styles.directoryTextBox());
      grid.setWidget(1, 1, directoryTextBox_);
      
      fileNameTextBox_ = new TextBox();
      fileNameTextBox_.setText(context.getUniqueFileStem());
      fileNameTextBox_.setStylePrimaryName(styles.fileNameTextBox());
      FormLabel fileNameLabel = new FormLabel("File name:", fileNameTextBox_);
      fileNameLabel.setStylePrimaryName(styles.fileNameLabel());
      grid.setWidget(2, 0, fileNameLabel);
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
      
      return ExportPlotUtils.composeTargetPath(ext, fileNameTextBox_, directory_);  
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
      String dirLabel = ExportPlotUtils.shortDirectoryName(directory, 250);
      directoryTextBox_.setText(dirLabel);
   }
   
   
   private ListBox imageFormatListBox_;
   private TextBox fileNameTextBox_;
   private FileSystemItem directory_;
   private TextBox directoryTextBox_;
  
   
   private final SavePlotAsImageContext context_;
 
   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();
   
   private final FileDialogs fileDialogs_ = 
      RStudioGinjector.INSTANCE.getFileDialogs();
}
