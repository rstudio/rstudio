package org.rstudio.studio.client.workbench.views.plots.ui.export;

import java.util.HashMap;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotExportContext;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class ExportTargetWidget extends Composite 
{
   public ExportTargetWidget(String defaultFormat,
                             PlotExportContext context,
                             final FileDialogs fileDialogs,
                             final RemoteFileSystemContext fileSystemContext)
   {
      context_ = context;
      
      ExportPlotDialogResources.Styles styles = 
                              ExportPlotDialogResources.INSTANCE.styles();

      
      Grid grid = new Grid(3, 2);
      grid.setCellPadding(0);
    
      Label imageFormatLabel = new Label("Image format:");
      imageFormatLabel.setStylePrimaryName(styles.exportTargetLabel());
          
      grid.setWidget(0, 0, imageFormatLabel);
      imageFormatListBox_ = new ListBox();
      JsArrayString formats = context.getFormats();
      int selectedIndex = 0;
      for (int i=0; i<formats.length(); i++)
      {
         String format = formats.get(i);
         if (format.equals(defaultFormat))
            selectedIndex = i;
         imageFormatListBox_.addItem(formats.get(i));
      }
      imageFormatListBox_.setSelectedIndex(selectedIndex);
      imageFormatListBox_.setStylePrimaryName(styles.imageFormatListBox());
      grid.setWidget(0, 1, imageFormatListBox_);
      
      Label fileNameLabel = new Label("File name:");
      imageFormatLabel.setStylePrimaryName(styles.exportTargetLabel());
      grid.setWidget(1, 0, fileNameLabel);
      fileNameTextBox_ = new TextBox();
      fileNameTextBox_.setText(context.getFilename());
      fileNameTextBox_.setStylePrimaryName(styles.fileNameTextBox());
      grid.setWidget(1, 1, fileNameTextBox_);
      
      
      ThemedButton directoryButton = new ThemedButton("Directory...");
      directoryButton.setStylePrimaryName(styles.directoryButton());
      directoryButton.getElement().getStyle().setMarginLeft(-2, Unit.PX);
      grid.setWidget(2, 0, directoryButton);
      directoryButton.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            fileDialogs.chooseFolder(
               "Choose Directory",
               fileSystemContext,
               FileSystemItem.createDir(directoryLabel_.getTitle().trim()),
               new ProgressOperationWithInput<FileSystemItem>() {

                 public void execute(FileSystemItem input,
                                     ProgressIndicator indicator)
                 {
                    if (input == null)
                       return;
                    
                    indicator.onCompleted();
                    
                    // update cache
                    initialDirectories_.put(context_.getDirectory().getPath(),
                                            input);
                    
                    // set displaye
                    setDirectoryText(input);  
                 }          
               });
         }
      });
      
     
      directoryLabel_ = new Label();
      setDirectoryText(getInitialDirectory());
      directoryLabel_.setStylePrimaryName(styles.directoryLabel());
      grid.setWidget(2, 1, directoryLabel_);
        
      initWidget(grid);
   }
   
   public void setInitialFocus()
   {
      fileNameTextBox_.setFocus(true);
      fileNameTextBox_.selectAll();
   }

   public String getFormat()
   {
      return imageFormatListBox_.getValue(
                                 imageFormatListBox_.getSelectedIndex());
   }
   
   private FileSystemItem getInitialDirectory()
   {
      // do we have a cached initial dir?
      String directory = context_.getDirectory().getPath();
      if (initialDirectories_.containsKey(directory))
         return initialDirectories_.get(directory);
      else
         return context_.getDirectory();
   }
   
   private void setDirectoryText(FileSystemItem directory)
   {
      // measure HTML and truncate if necessary
      String path = directory.getPath();
      Size textSize = DomMetrics.measureHTML(path, "gwt-Label");
      if (textSize.width >= 250)
      {
         // shortened directory nam
         if (directory.getParentPath() != null &&
             directory.getParentPath().getParentPath() != null)
         {
            path = ".../" + 
                   directory.getParentPath().getName() + "/" +
                   directory.getName(); 
         }
      }
      
      // set label
      directoryLabel_.setText(path);
      
      // set tooltip
      directoryLabel_.setTitle(directory.getPath());
   }
   
   
   private ListBox imageFormatListBox_;
   private TextBox fileNameTextBox_;
   private Label directoryLabel_;
   
   private final PlotExportContext context_;
   
   // remember what directory was chosen for plot export for various
   // working directories
   static HashMap<String, FileSystemItem> initialDirectories_ = 
                                       new HashMap<String,FileSystemItem>();
   
   
}
