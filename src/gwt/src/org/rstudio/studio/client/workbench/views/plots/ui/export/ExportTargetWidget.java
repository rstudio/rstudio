package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class ExportTargetWidget extends Composite 
{
   public ExportTargetWidget()
   {
      ExportPlotDialogResources.Styles styles = 
                              ExportPlotDialogResources.INSTANCE.styles();

      
      Grid grid = new Grid(3, 2);
      grid.setCellPadding(0);
    
      Label imageFormatLabel = new Label("Image format:");
      imageFormatLabel.setStylePrimaryName(styles.exportTargetLabel());
          
      grid.setWidget(0, 0, imageFormatLabel);
      imageFormatListBox_ = new ListBox();
      imageFormatListBox_.addItem("PNG");
      imageFormatListBox_.setSelectedIndex(0);
      imageFormatListBox_.setStylePrimaryName(styles.imageFormatListBox());
      grid.setWidget(0, 1, imageFormatListBox_);
      
      Label fileNameLabel = new Label("File name:");
      imageFormatLabel.setStylePrimaryName(styles.exportTargetLabel());
      grid.setWidget(1, 0, fileNameLabel);
      fileNameTextBox_ = new TextBox();
      fileNameTextBox_.setStylePrimaryName(styles.fileNameTextBox());
      grid.setWidget(1, 1, fileNameTextBox_);
      
      
      ThemedButton directoryButton = new ThemedButton("Directory...");
      directoryButton.setStylePrimaryName(styles.directoryButton());
      directoryButton.getElement().getStyle().setMarginLeft(-2, Unit.PX);
      grid.setWidget(2, 0, directoryButton);
      
      directoryTextBox_ = new TextBox();
      directoryTextBox_.setReadOnly(true);
      directoryTextBox_.setStylePrimaryName(styles.directoryTextBox());
      grid.setWidget(2, 1, directoryTextBox_);
      
      directoryTextBox_.setText("~/Projects/Analysis");
      
      
      initWidget(grid);
   }

   
   
   private ListBox imageFormatListBox_;
   private TextBox fileNameTextBox_;
   private TextBox directoryTextBox_;

}
