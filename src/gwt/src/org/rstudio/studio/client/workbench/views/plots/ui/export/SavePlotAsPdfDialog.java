package org.rstudio.studio.client.workbench.views.plots.ui.export;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ModalDialogProgressIndicator;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class SavePlotAsPdfDialog extends ModalDialogBase
{
   public SavePlotAsPdfDialog(GlobalDisplay globalDisplay,
                              PlotsServerOperations server,
                              FileSystemItem defaultDirectory,
                              String defaultPlotName,
                              final SavePlotAsPdfOptions options,
                              final OperationWithInput<SavePlotAsPdfOptions> onClose)
   {
      setText("Save Plot as PDF");
      
      globalDisplay_ = globalDisplay;
      server_ = server;
      defaultDirectory_ = defaultDirectory;
      defaultPlotName_ = defaultPlotName;
      options_ = options;
      
      paperSizes_.add(new PaperSize("US Letter", 8.5, 11));
      paperSizes_.add(new PaperSize("US Legal", 8.5, 14));
      paperSizes_.add(new PaperSize("A4", 8.27, 11.69));
      paperSizes_.add(new PaperSize("A5", 5.83, 8.27));
      paperSizes_.add(new PaperSize("A6", 4.13, 5.83));
      paperSizes_.add(new PaperSize("4 x 6 in.", 4, 6));
      paperSizes_.add(new PaperSize("5 x 7 in.", 5, 7));
      paperSizes_.add(new PaperSize("6 x 8 in.", 6, 8));
      
      progressIndicator_ = addProgressIndicator();
      
      ThemedButton saveButton = new ThemedButton("Save", 
                                                 new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            attemptSavePdf(false, new Operation() {
               @Override
               public void execute()
               {
                  // get options to send back to caller for persistence
                  PaperSize paperSize = selectedPaperSize();
                  SavePlotAsPdfOptions pdfOptions = SavePlotAsPdfOptions.create(
                                             paperSize.getWidth(),
                                             paperSize.getHeight(),
                                             isPortraitOrientation(),
                                             viewAfterSaveCheckBox_.getValue());
               
                  onClose.execute(pdfOptions);
                  
                  closeDialog();   
               }
            });
         }
      });
      addOkButton(saveButton);
      addCancelButton();
   }
   
   @Override 
   protected void onDialogShown()
   {
      fileNameTextBox_.setFocus(true);
      fileNameTextBox_.selectAll();
   }

   @Override
   protected Widget createMainWidget()
   {
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();
      
      Grid grid = new Grid(6, 2);
      grid.setStylePrimaryName(styles.savePdfMainWidget());
      final int kComponentSpacing = 7;    
      
      // paper size
      grid.setWidget(0, 0, new Label("PDF Size:"));
      HorizontalPanel paperSizePanel = new HorizontalPanel();
      paperSizePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      paperSizePanel.setSpacing(kComponentSpacing);
       
      // paper size list box
      int selectedPaperSize = 0;
      paperSizeListBox_ = new ListBox();
      paperSizeListBox_.setStylePrimaryName(styles.savePdfSizeListBox());
      for (int i=0; i<paperSizes_.size(); i++)
      {
         PaperSize paperSize = paperSizes_.get(i);
         paperSizeListBox_.addItem(paperSize.getName());
         if (paperSize.getWidth() == options_.getWidth() &&
             paperSize.getHeight() == options_.getHeight())
         {
            selectedPaperSize = i;
         }
      }
      paperSizeListBox_.addChangeHandler(new ChangeHandler() {
         public void onChange(ChangeEvent event)
         {
            updateSizeDescription();  
         } 
      });
      paperSizePanel.add(paperSizeListBox_);
      
      // paper size label
      paperSizeLabel_ = new Label();
      paperSizeLabel_.setStylePrimaryName(styles.savePdfSizeLabel());
      paperSizePanel.add(paperSizeLabel_);
      
      // add to grid
      grid.setWidget(0, 1, paperSizePanel);
      
      // orientation
      grid.setWidget(1, 0, new Label("Orientation:"));
      HorizontalPanel orientationPanel = new HorizontalPanel();
      orientationPanel.setSpacing(kComponentSpacing);
      VerticalPanel orientationGroupPanel = new VerticalPanel();
      final String kOrientationGroup = new String("Orientation");
      portraitRadioButton_ = new RadioButton(kOrientationGroup, "Portrait");
      orientationGroupPanel.add(portraitRadioButton_);
      landscapeRadioButton_ = new RadioButton(kOrientationGroup, "Landscape");
      orientationGroupPanel.add(landscapeRadioButton_);
      orientationPanel.add(orientationGroupPanel);
      grid.setWidget(1, 1, orientationPanel);
      
      grid.setWidget(2, 0, new HTML("&nbsp;"));
      
      ThemedButton directoryButton = new ThemedButton("Directory...");
      directoryButton.setStylePrimaryName(styles.directoryButton());
      directoryButton.getElement().getStyle().setMarginLeft(-2, Unit.PX);
      grid.setWidget(3, 0, directoryButton);
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
                    ExportPlot.setDefaultSaveDirectory(defaultDirectory_,
                                                       input);
                    
                    // set display
                    setDirectory(input);  
                 }          
               });
         }
      });
      
      
      directoryLabel_ = new Label();
      setDirectory(defaultDirectory_);
      directoryLabel_.setStylePrimaryName(styles.savePdfDirectoryLabel());
      grid.setWidget(3, 1, directoryLabel_);
      
      Label fileNameLabel = new Label("File name:");
      fileNameLabel.setStylePrimaryName(styles.savePdfFileNameLabel());
      grid.setWidget(4, 0, fileNameLabel);
      fileNameTextBox_ = new TextBox();
      fileNameTextBox_.setText(defaultPlotName_);
      fileNameTextBox_.setStylePrimaryName(styles.savePdfFileNameTextBox());
      grid.setWidget(4, 1, fileNameTextBox_);
      
      
      // view after size
      viewAfterSaveCheckBox_ = new CheckBox("View plot after saving");
      viewAfterSaveCheckBox_.setStylePrimaryName(
                                       styles.savePdfViewAfterCheckbox());
      viewAfterSaveCheckBox_.setValue(options_.getViewAfterSave());
      grid.setWidget(5, 1, viewAfterSaveCheckBox_);
      
      // set default values
      paperSizeListBox_.setSelectedIndex(selectedPaperSize);
      updateSizeDescription();
      if (options_.getPortrait())
         portraitRadioButton_.setValue(true);
      else
         landscapeRadioButton_.setValue(true);
      
      // return the widget
      return grid;
   }
   
   private void attemptSavePdf(boolean overwrite,
                               final Operation onCompleted)
   {
      // validate file name
      FileSystemItem targetPath = getTargetPath();
      if (targetPath == null)
      {
         globalDisplay_.showErrorMessage(
            "File Name Required", 
            "You must provide a file name for the plot pdf.", 
            fileNameTextBox_);
         return;
      }
      
      // create handler
      SavePlotAsHandler handler = new SavePlotAsHandler(
            globalDisplay_, 
            progressIndicator_, 
            new SavePlotAsHandler.ServerOperations()
            {
               @Override
               public void savePlot(
                     FileSystemItem targetPath, 
                     boolean overwrite,
                     ServerRequestCallback<Bool> requestCallback)
               {
                  PaperSize paperSize = selectedPaperSize();
                  double width = paperSize.getWidth();
                  double height = paperSize.getHeight();
                  if (!isPortraitOrientation())
                  {
                     width = paperSize.getHeight();
                     height = paperSize.getWidth();
                  }
                  
                  server_.savePlotAsPdf(targetPath, 
                                        width,
                                        height,
                                        overwrite,
                                        requestCallback);
               }

               @Override
               public String getFileUrl(FileSystemItem path)
               {
                  return server_.getFileUrl(path);
               }
            });
      
      // invoke handler
      handler.attemptSave(targetPath, 
                          overwrite, 
                          viewAfterSaveCheckBox_.getValue(), 
                          onCompleted);      
   }
   
   private FileSystemItem getTargetPath()
   { 
      return ExportPlot.composeTargetPath(".pdf", fileNameTextBox_, directory_);  
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
   
   
   private PaperSize selectedPaperSize()
   {
      int selectedSize =  paperSizeListBox_.getSelectedIndex();
      return paperSizes_.get(selectedSize);
   }
   
   private boolean isPortraitOrientation()
   {
      return portraitRadioButton_.getValue();
   }
   
   private void updateSizeDescription()
   {
      paperSizeLabel_.setText(selectedPaperSize().getSizeDescription()); 
   }
   
   private class PaperSize
   {
      public PaperSize(String name, double width, double height)
      {
         name_ = name;
         width_ = width;
         height_ = height;
      }
      
      public String getName() { return name_; }
      public double getWidth() { return width_; }
      public double getHeight() { return height_; }
      
      public String getSizeDescription()
      {
         return sizeFormat_.format(getWidth()) + 
                " by " + 
                sizeFormat_.format(getHeight()) +
                " inches";
      }
      
      private final String name_ ;
      private final double width_ ;
      private final double height_ ;
      private final NumberFormat sizeFormat_ = NumberFormat.getFormat("##0.00");
   }
   
   private final List<PaperSize> paperSizes_ = new ArrayList<PaperSize>(); 
   
   private final GlobalDisplay globalDisplay_;
   private final PlotsServerOperations server_;
   private final SavePlotAsPdfOptions options_;
   private final FileSystemItem defaultDirectory_;
   private final String defaultPlotName_;
   private final ModalDialogProgressIndicator progressIndicator_;
   
   private TextBox fileNameTextBox_;
   private FileSystemItem directory_;
   private Label directoryLabel_;
   private Label paperSizeLabel_ ;
   private ListBox paperSizeListBox_;
   private RadioButton portraitRadioButton_ ;
   private RadioButton landscapeRadioButton_;
   private CheckBox viewAfterSaveCheckBox_;
   
   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();
   
   private final FileDialogs fileDialogs_ = 
      RStudioGinjector.INSTANCE.getFileDialogs();
  
}
