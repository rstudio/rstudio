/*
 * SavePlotAsImageDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.exportplot;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.exportplot.model.SavePlotAsImageContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class SavePlotAsImageDialog extends ExportPlotDialog
{
   public SavePlotAsImageDialog(
                           GlobalDisplay globalDisplay,
                           SavePlotAsImageOperation saveOperation,
                           ExportPlotPreviewer previewer,
                           SavePlotAsImageContext context,
                           final ExportPlotOptions options,
                           final OperationWithInput<ExportPlotOptions> onClose)
   {
      super(options, previewer);

      setText(constants_.savePlotAsImageText());

      globalDisplay_ = globalDisplay;
      saveOperation_ = saveOperation;
      context_ = context;
      progressIndicator_ = addProgressIndicator();

      ThemedButton saveButton = new ThemedButton(constants_.saveTitle() + "...",
                                                 new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            attemptSavePlot(new Operation() {
               @Override
               public void execute()
               {
                  onClose.execute(getCurrentOptions(options));

                  closeDialog();
               }
            });
         }
      });
      addOkButton(saveButton);
      addCancelButton();

      // file type
      saveAsTarget_ = new SavePlotAsImageTargetEditor(options.getFormat(),
                                                      context);

      // view after save
      viewAfterSaveCheckBox_ = new CheckBox(constants_.viewAfterSaveCheckBoxTitle());
      viewAfterSaveCheckBox_.setValue(options.getViewAfterSave());

      // use device pixel ratio
      useDevicePixelRatioCheckBox_ = new CheckBox(constants_.useDevicePixelRatioCheckBoxLabel());
      useDevicePixelRatioCheckBox_.setTitle(constants_.useDevicePixelRatioCheckBoxTitle());
      useDevicePixelRatioCheckBox_.setValue(options.getUseDevicePixelRatio());
      useDevicePixelRatioCheckBox_.getElement().getStyle().setPaddingLeft(6, Unit.PX);

      // resolution of saved bitmaps, which replaces the device pixel ratio
      // option where the saved image is drawn by R (see supportsResolution)
      resolutionListBox_ = new ListBox();
      resolutionListBox_.getElement().setId(ElementIds.getElementId(ElementIds.EXPORT_PLOT_RESOLUTION));
      // an explicit resolution is listed even when it matches the screen's,
      // since the screen's changes with the display
      resolutionListBox_.addItem(constants_.screenResolutionText(Integer.toString(screenResolution())), "0");
      for (int dpi : RESOLUTIONS)
         resolutionListBox_.addItem(constants_.resolutionText(Integer.toString(dpi)), Integer.toString(dpi));
      resolutionListBox_.setSelectedIndex(0);
      for (int i = 0; i < resolutionListBox_.getItemCount(); i++)
      {
         if (resolutionListBox_.getValue(i).equals(Integer.toString(options.getResolution())))
            resolutionListBox_.setSelectedIndex(i);
      }
      resolutionListBox_.addChangeHandler(event -> updateSizeText());

      sizeText_ = new Label();
      sizeText_.getElement().setId(ElementIds.getElementId(ElementIds.EXPORT_PLOT_SIZE_TEXT));
      sizeText_.getElement().getStyle().setMarginLeft(10, Unit.PX);

      // update preview button
      ThemedButton updatePreviewButton = new ThemedButton(
            constants_.updatePreviewTitle(),
            new ClickHandler() {
               public void onClick(ClickEvent event)
               {
                  getSizeEditor().updatePreview();
               }
            });
      addLeftWidget(updatePreviewButton);
   }

   @Override
   protected Widget createMainWidget()
   {
      Widget mainWidget = super.createMainWidget();
      getSizeEditor().setUpdateButtonVisible(false);
      return mainWidget;
   }

   @Override
   protected Widget createTopLeftWidget()
   {
      return saveAsTarget_;
   }

   @Override
   protected Widget createBottomWidget()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      panel.add(viewAfterSaveCheckBox_);
      if (!supportsResolution())
      {
         panel.add(useDevicePixelRatioCheckBox_);
         return panel;
      }

      FormLabel resolutionLabel = new FormLabel(constants_.resolutionLabel(), resolutionListBox_);
      resolutionLabel.getElement().getStyle().setMarginLeft(16, Unit.PX);
      resolutionLabel.getElement().getStyle().setMarginRight(5, Unit.PX);
      panel.add(resolutionLabel);
      panel.add(resolutionListBox_);
      panel.add(sizeText_);

      getSizeEditor().addSizeChangedHandler(() -> updateSizeText());
      saveAsTarget_.addChangeHandler(event -> updateSizeText());
      updateSizeText();
      return panel;
   }

   /**
    * Whether the dialog offers a resolution for saved images; otherwise, it
    * offers whether to use the display's device pixel ratio.
    */
   protected boolean supportsResolution()
   {
      return true;
   }

   // The selected resolution in DPI; 0 means the display's.
   private int getResolution()
   {
      if (!supportsResolution())
         return 0;
      return StringUtil.parseInt(resolutionListBox_.getSelectedValue(), 0);
   }

   private boolean getUseDevicePixelRatio()
   {
      if (!supportsResolution())
         return useDevicePixelRatioCheckBox_.getValue();
      return getResolution() == 0;
   }

   private static int screenResolution()
   {
      return (int) Math.round(96 * BrowseCap.devicePixelRatio());
   }

   // Shows the physical size of the saved image, and its size in pixels
   // (plot sizes are in pixels at 96 DPI; the resolution scales them).
   private void updateSizeText()
   {
      boolean bitmap = saveAsTarget_.isBitmapFormat();
      resolutionListBox_.setEnabled(bitmap);

      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      int width = sizeEditor.getImageWidth();
      int height = sizeEditor.getImageHeight();
      NumberFormat inches = NumberFormat.getFormat("0.##");
      String widthInches = inches.format(width / 96.0);
      String heightInches = inches.format(height / 96.0);
      if (!bitmap)
      {
         sizeText_.setText(constants_.exportSizeInchesText(widthInches, heightInches));
         return;
      }

      // the session scales by the exact pixel ratio for the screen's resolution
      double scale = getResolution() == 0
            ? BrowseCap.devicePixelRatio()
            : getResolution() / 96.0;
      sizeText_.setText(constants_.exportSizeText(
            widthInches,
            heightInches,
            Integer.toString((int) (width * scale)),
            Integer.toString((int) (height * scale))));
   }

   @Override
   protected ExportPlotOptions getCurrentOptions(ExportPlotOptions previous)
   {
      ExportPlotSizeEditor sizeEditor = getSizeEditor();
      return ExportPlotOptions.create(sizeEditor.getImageWidth(),
                                      sizeEditor.getImageHeight(),
                                      sizeEditor.getKeepRatio(),
                                      saveAsTarget_.getFormat(),
                                      viewAfterSaveCheckBox_.getValue(),
                                      getUseDevicePixelRatio(),
                                      previous.getCopyAsMetafile(),
                                      supportsResolution() ? getResolution() : previous.getResolution());
   }

   private void attemptSavePlot(final Operation onCompleted)
   {
      // compose default file path
      String ext = saveAsTarget_.getDefaultExtension();
      FileSystemItem defaultDir = ExportPlotUtils.getDefaultSaveDirectory(
                                       context_.getDirectory());
      FileSystemItem initialPath = FileSystemItem.createFile(
                                       defaultDir.completePath(
                                          context_.getUniqueFileStem() + ext));

      fileDialogs_.saveFile(
         constants_.savePlotAsImageText(),
         fileSystemContext_,
         initialPath,
         ext,
         false,
         new ProgressOperationWithInput<FileSystemItem>() {
            @Override
            public void execute(FileSystemItem input, ProgressIndicator indicator)
            {
               if (input == null)
               {
                  indicator.onCompleted();
                  return;
               }

               indicator.onCompleted();

               // update default save directory
               ExportPlotUtils.setDefaultSaveDirectory(input.getParentPath());

               // determine format from the chosen file extension
               String format = saveAsTarget_.getFormat();

               saveOperation_.attemptSave(
                     progressIndicator_,
                     input,
                     format,
                     getSizeEditor(),
                     true, // overwrite (user confirmed via save dialog)
                     viewAfterSaveCheckBox_.getValue(),
                     getUseDevicePixelRatio(),
                     getResolution(),
                     onCompleted);
            }
         });
   }

   private final GlobalDisplay globalDisplay_;
   private ProgressIndicator progressIndicator_;
   private final SavePlotAsImageOperation saveOperation_;
   private final SavePlotAsImageContext context_;
   private SavePlotAsImageTargetEditor saveAsTarget_;
   private CheckBox viewAfterSaveCheckBox_;
   private CheckBox useDevicePixelRatioCheckBox_;
   private ListBox resolutionListBox_;
   private Label sizeText_;

   private static final int[] RESOLUTIONS = { 96, 150, 300, 600 };

   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();

   private final FileDialogs fileDialogs_ =
      RStudioGinjector.INSTANCE.getFileDialogs();

   private static final ExportPlotConstants constants_ = GWT.create(ExportPlotConstants.class);
}
