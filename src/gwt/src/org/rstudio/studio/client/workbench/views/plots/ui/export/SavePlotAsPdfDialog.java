/*
 * SavePlotAsPdfDialog.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.FieldSetWrapperPanel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.Bool;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotUtils;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.plots.PlotsConstants;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
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
                              final SessionInfo sessionInfo,
                              FileSystemItem defaultDirectory,
                              String defaultPlotName,
                              final SavePlotAsPdfOptions options,
                              double plotWidth,
                              double plotHeight,
                              final OperationWithInput<SavePlotAsPdfOptions> onClose)
   {
      super(Roles.getDialogRole());
      setThemeAware(true);
      setText(constants_.savePlotPDFText());

      globalDisplay_ = globalDisplay;
      sessionInfo_ = sessionInfo;
      server_ = server;
      defaultDirectory_ = defaultDirectory;
      defaultPlotName_ = defaultPlotName;
      options_ = options;
      plotWidth_ = plotWidth;
      plotHeight_ = plotHeight;

      progressIndicator_ = addProgressIndicator();

      ThemedButton saveButton = new ThemedButton(constants_.saveTitle() + "...",
                                                 new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            attemptSavePdf(new Operation() {
               @Override
               public void execute()
               {
                  // get options to send back to caller for persistence
                  PaperSize paperSize = paperSizeEditor_.selectedPaperSize();
                  SavePlotAsPdfOptions pdfOptions = SavePlotAsPdfOptions.create(
                                             paperSize.getWidth(),
                                             paperSize.getHeight(),
                                             isPortraitOrientation(),
                                             useCairoPdf(),
                                             viewAfterSaveCheckBox_.getValue());

                  onClose.execute(pdfOptions);

                  closeDialog();
               }
            });
         }
      });
      addOkButton(saveButton);
      addCancelButton();

      // view after save
      viewAfterSaveCheckBox_ = new CheckBox(constants_.viewPlotAfterSavingLabel());
      viewAfterSaveCheckBox_.setValue(options.getViewAfterSave());

      ThemedButton previewButton =  new ThemedButton(constants_.previewTitle(),
                                                     new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.getPlotTempdir(new SimpleRequestCallback<String>()
            {
               @Override
               public void onResponseReceived(String response)
               {
                  FileSystemItem tempDir =
                        FileSystemItem.createDir(response);

                  // get temp file for preview
                  FileSystemItem previewPath =
                        FileSystemItem.createFile(tempDir.completePath("preview.pdf"));

                  // invoke handler
                  SavePlotAsHandler handler = createSavePlotAsHandler();
                  handler.attemptSave(previewPath, true, true, null);
               }
            });
         }
      });
      addLeftButton(previewButton, ElementIds.PREVIEW_BUTTON);
   }

   @Override
   protected Widget createMainWidget()
   {
      ExportPlotResources.Styles styles = ExportPlotResources.INSTANCE.styles();

      LayoutGrid grid = new LayoutGrid(4, 2);
      grid.addStyleName(styles.savePdfMainWidget());

      // paper size
      Label sizeLabel = new Label(constants_.pdfSizeText());
      grid.setWidget(0, 0, sizeLabel);

      // paper size label
      paperSizeEditor_ = new PaperSizeEditor(sizeLabel);
      grid.setWidget(0, 1, paperSizeEditor_);

      // orientation
      Label orientationLabel = new Label(constants_.orientationText());
      grid.setWidget(1, 0, orientationLabel);
      VerticalPanel orientationGroupPanel = new VerticalPanel();
      FieldSetWrapperPanel<VerticalPanel> orientationButtons =
            new FieldSetWrapperPanel<>(orientationGroupPanel, orientationLabel);
      final String kOrientationGroup = constants_.orientationLabel();
      portraitRadioButton_ = new RadioButton(kOrientationGroup, constants_.portraitLabel());
      orientationGroupPanel.add(portraitRadioButton_);
      landscapeRadioButton_ = new RadioButton(kOrientationGroup, constants_.landscapeLabel());
      orientationGroupPanel.add(landscapeRadioButton_);
      grid.setWidget(1, 1, orientationButtons);

      boolean haveCairoPdf = sessionInfo_.isCairoPdfAvailable();
      if (haveCairoPdf)
         grid.setWidget(2, 0, new Label(constants_.optionsText()));
      String label = constants_.useCairoPdfDeviceLabel();
      if (BrowseCap.isMacintoshDesktop())
         label = constants_.requiresX11Label(label);
      chkCairoPdf_ = new CheckBox(label);
      chkCairoPdf_.setValue(haveCairoPdf && options_.getCairoPdf());
      if (haveCairoPdf)
         grid.setWidget(2, 1, chkCairoPdf_);

      // view after save
      grid.setWidget(3, 1, viewAfterSaveCheckBox_);

      // set default value
      if (options_.getPortrait())
         portraitRadioButton_.setValue(true);
      else
         landscapeRadioButton_.setValue(true);

      // return the widget
      return grid;
   }

   private void attemptSavePdf(final Operation onCompleted)
   {
      // compose default file path
      FileSystemItem defaultDir = ExportPlotUtils.getDefaultSaveDirectory(
                                       defaultDirectory_);
      FileSystemItem initialPath = FileSystemItem.createFile(
                                       defaultDir.completePath(
                                          defaultPlotName_ + ".pdf"));

      fileDialogs_.saveFile(
         constants_.savePlotPDFText(),
         fileSystemContext_,
         initialPath,
         ".pdf",
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

               // invoke handler
               SavePlotAsHandler handler = createSavePlotAsHandler();
               handler.attemptSave(
                     input,
                     true, // overwrite (user confirmed via save dialog)
                     viewAfterSaveCheckBox_.getValue(),
                     onCompleted);
            }
         });
   }

   private boolean isPortraitOrientation()
   {
      return portraitRadioButton_.getValue();
   }

   private boolean useCairoPdf()
   {
      return chkCairoPdf_.getValue();
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

      private final String name_;
      private final double width_;
      private final double height_;
   }

   private SavePlotAsHandler createSavePlotAsHandler()
   {
      return new SavePlotAsHandler(
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
               PaperSize paperSize = paperSizeEditor_.selectedPaperSize();
               double width = paperSize.getWidth();
               double height = paperSize.getHeight();

               // This is more intuitive for the user to get an aspect ratio based on
               // portrait/landscape selection regardless of the paper size's aspect ratio
               width = isPortraitOrientation() ? Math.min(paperSize.getHeight(), paperSize.getWidth())
                  : Math.max(paperSize.getHeight(), paperSize.getWidth());
               height = isPortraitOrientation() ? Math.max(paperSize.getHeight(), paperSize.getWidth())
                  : Math.min(paperSize.getHeight(), paperSize.getWidth());

               server_.savePlotAsPdf(targetPath,
                                     width,
                                     height,
                                     chkCairoPdf_.getValue(),
                                     overwrite,
                                     requestCallback);
            }

            @Override
            public String getFileUrl(FileSystemItem path)
            {
               return server_.getFileUrl(path);
            }
         });
   }

   private class PaperSizeEditor extends Composite
   {
      public PaperSizeEditor(Label visibleLabel)
      {
         ExportPlotResources.Styles styles =
                                       ExportPlotResources.INSTANCE.styles();

         paperSizes_.add(new PaperSize("US Letter", 8.5, 11));
         paperSizes_.add(new PaperSize("US Legal", 8.5, 14));
         paperSizes_.add(new PaperSize("A4", 8.27, 11.69));
         paperSizes_.add(new PaperSize("A5", 5.83, 8.27));
         paperSizes_.add(new PaperSize("A6", 4.13, 5.83));
         paperSizes_.add(new PaperSize("4 x 6 in.", 4, 6));
         paperSizes_.add(new PaperSize("5 x 7 in.", 5, 7));
         paperSizes_.add(new PaperSize("6 x 8 in.", 6, 8));

         FieldSetWrapperPanel<HorizontalPanel> panel = new FieldSetWrapperPanel<>(
               new HorizontalPanel(), visibleLabel);
         panel.getPanel().setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

         // paper size list box
         int selectedPaperSize = -1;
         paperSizeListBox_ = new ListBox();
         paperSizeListBox_.addStyleName(styles.savePdfSizeListBox());
         Roles.getListboxRole().setAriaLabelProperty(paperSizeListBox_.getElement(), constants_.sizePresetLabel());
         for (int i = 0; i < paperSizes_.size(); i++)
         {
            PaperSize paperSize = paperSizes_.get(i);
            paperSizeListBox_.addItem(paperSize.getName());
            if (paperSize.getWidth() == options_.getWidth() &&
                paperSize.getHeight() == options_.getHeight())
            {
               selectedPaperSize = i;
            }
         }
         PaperSize customPaperSize = new PaperSize(constants_.deviceSizeName(),
                                                   plotWidth_,
                                                   plotHeight_);
         paperSizes_.add(customPaperSize);
         paperSizeListBox_.addItem(customPaperSize.getName());

         if (selectedPaperSize == -1)
         {
            setCustomPaperSize(plotWidth_, plotHeight_);
            selectedPaperSize = paperSizes_.size() - 1;
         }

         paperSizeListBox_.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event)
            {
               updateSizeDescription();
            }
         });
         panel.add(paperSizeListBox_);

         HorizontalPanel editPanel = new HorizontalPanel();
         widthTextBox_ = new TextBox();
         widthTextBox_.addStyleName(styles.savePdfPaperSizeTextBox());
         Roles.getTextboxRole().setAriaLabelProperty(widthTextBox_.getElement(), constants_.widthLabel());
         widthTextBox_.addChangeHandler(sizeTextBoxChangeHandler_);
         editPanel.add(widthTextBox_);

         Label label = new Label();
         label.getElement().setInnerSafeHtml(SafeHtmlUtils.fromSafeConstant("&times;"));
         label.setStylePrimaryName(styles.savePdfPaperSizeX());
         editPanel.add(label);

         heightTextBox_ = new TextBox();
         heightTextBox_.addStyleName(styles.savePdfPaperSizeTextBox());
         Roles.getTextboxRole().setAriaLabelProperty(heightTextBox_.getElement(), constants_.heightLabel());
         heightTextBox_.addChangeHandler(sizeTextBoxChangeHandler_);
         editPanel.add(heightTextBox_);
         panel.add(editPanel);

         Label inchesLabel = new Label(constants_.inchesLabel());
         inchesLabel.setStylePrimaryName(styles.savePdfPaperSizeX());
         editPanel.add(inchesLabel);

         paperSizeListBox_.setSelectedIndex(selectedPaperSize);
         updateSizeDescription();

         initWidget(panel);
      }

      public PaperSize selectedPaperSize()
      {
         int selectedSize =  paperSizeListBox_.getSelectedIndex();
         return paperSizes_.get(selectedSize);
      }

      private void updateSizeDescription()
      {
         setPaperSize(selectedPaperSize());
      }

      private void setPaperSize(PaperSize paperSize)
      {
         widthTextBox_.setText(sizeFormat_.format(paperSize.getWidth()));
         heightTextBox_.setText(sizeFormat_.format(paperSize.getHeight()));
      }

      private void setCustomPaperSize(double width, double height)
      {
         paperSizes_.remove(paperSizes_.size() - 1);
         paperSizes_.add(new PaperSize(constants_.customLabel(), width, height));
      }

      private ChangeHandler sizeTextBoxChangeHandler_ = new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            // read width and height
            PaperSize defaultSize = selectedPaperSize();
            double width = readSizeEntry(widthTextBox_, defaultSize.getWidth());
            double height = readSizeEntry(heightTextBox_,
                                          defaultSize.getHeight());

            // see if it matches an existing size
            int sizeIndex = -1;
            for (int i=0; i<paperSizes_.size(); i++)
            {
               PaperSize paperSize = paperSizes_.get(i);
               if (paperSize.getWidth() == width &&
                   paperSize.getHeight() == height)
               {
                  sizeIndex = i;
                  break;
               }
            }

            // if it doesn't then update custom
            if (sizeIndex == -1)
            {
               setCustomPaperSize(width, height);
               sizeIndex = paperSizes_.size() - 1;
               paperSizeListBox_.setItemText(sizeIndex, constants_.customLabel());
            }

            // select
            paperSizeListBox_.setSelectedIndex(sizeIndex);
         }
      };

      private double readSizeEntry(TextBox textBox, double defaultValue)
      {
         double size = defaultValue;
         try
         {
            size = Double.parseDouble(textBox.getText().trim());

            if (size < kMimimumSize)
               size = defaultValue;
            else if (size > kMaximumSize)
               size = defaultValue;
         }
         catch(NumberFormatException e)
         {
         }
         textBox.setText(sizeFormat_.format(size));
         return size;
      }


      private ListBox paperSizeListBox_;
      private final TextBox widthTextBox_;
      private final TextBox heightTextBox_;
      private final List<PaperSize> paperSizes_ = new ArrayList<>();
      private final NumberFormat sizeFormat_ = NumberFormat.getFormat("##0.00");

      private final double kMimimumSize = 3.0;
      private final double kMaximumSize = 100.0;
   }



   private final GlobalDisplay globalDisplay_;
   private final SessionInfo sessionInfo_;
   private final PlotsServerOperations server_;
   private final SavePlotAsPdfOptions options_;
   private final double plotWidth_;
   private final double plotHeight_;
   private final FileSystemItem defaultDirectory_;
   private final String defaultPlotName_;
   private final ProgressIndicator progressIndicator_;

   private PaperSizeEditor paperSizeEditor_;

   private RadioButton portraitRadioButton_;
   private RadioButton landscapeRadioButton_;

   private CheckBox chkCairoPdf_;
   private CheckBox viewAfterSaveCheckBox_;


   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();

   private final FileDialogs fileDialogs_ =
      RStudioGinjector.INSTANCE.getFileDialogs();
   private static final PlotsConstants constants_ = com.google.gwt.core.client.GWT.create(PlotsConstants.class);

}
