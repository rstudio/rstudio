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

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
      panel.add(viewAfterSaveCheckBox_);
      panel.add(useDevicePixelRatioCheckBox_);
      return panel;
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
                                      useDevicePixelRatioCheckBox_.getValue(),
                                      previous.getCopyAsMetafile());
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
                     useDevicePixelRatioCheckBox_.getValue(),
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

   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();

   private final FileDialogs fileDialogs_ =
      RStudioGinjector.INSTANCE.getFileDialogs();

   private static final ExportPlotConstants constants_ = GWT.create(ExportPlotConstants.class);
}
