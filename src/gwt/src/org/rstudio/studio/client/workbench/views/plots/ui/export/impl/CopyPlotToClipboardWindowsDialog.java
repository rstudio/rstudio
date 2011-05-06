package org.rstudio.studio.client.workbench.views.plots.ui.export.impl;


import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlotResources;
import org.rstudio.studio.client.workbench.views.plots.ui.export.ExportPlotSizeEditor;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

public class CopyPlotToClipboardWindowsDialog extends CopyPlotToClipboardDesktopDialog
{

   public CopyPlotToClipboardWindowsDialog(
                                 PlotsServerOperations server,
                                 ExportPlotOptions options,
                                 OperationWithInput<ExportPlotOptions> onClose)
   {
      super(server, options, onClose);
     
      
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();
      
      Label label = new Label();
      label.setStylePrimaryName(styles.copyFormatLabel());
      label.setText("Copy as:");
      addLeftWidget(label);
      
      copyAsBitmapRadioButton_ = new RadioButton(
                                       "Format", 
                                       SafeHtmlUtils.fromString("Bitmap"));
      copyAsBitmapRadioButton_.setStylePrimaryName(styles.copyFormatBitmap());
      addLeftWidget(copyAsBitmapRadioButton_);
      
      copyAsMetafileRadioButton_ = new RadioButton(
                                       "Format", 
                                       SafeHtmlUtils.fromString("Metafile"));
      copyAsMetafileRadioButton_.setStylePrimaryName(styles.copyFormatMetafile());
      addLeftWidget(copyAsMetafileRadioButton_);
      
      if (options.getCopyAsMetafile())
         copyAsMetafileRadioButton_.setValue(true);
      else
         copyAsBitmapRadioButton_.setValue(true);
   }
   
   
   @Override
   protected void performCopy()
   {
      if (getCopyAsMetafile())
         copyAsMetafile();
      else
         copyAsBitmap();
   }
     
   
   @Override
   protected ExportPlotOptions getCurrentOptions(ExportPlotOptions previous)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      return ExportPlotOptions.create(sizeEditor.getImageWidth(), 
                                      sizeEditor.getImageHeight(), 
                                      sizeEditor.getKeepRatio(),
                                      previous.getFormat(),
                                      previous.getViewAfterSave(),
                                      getCopyAsMetafile());    
   }
   
   
   private boolean getCopyAsMetafile()
   {
      return copyAsMetafileRadioButton_.getValue();
   }
   
   private void copyAsMetafile()
   {
      
   }
   
   private RadioButton copyAsBitmapRadioButton_;
   private RadioButton copyAsMetafileRadioButton_;

}
