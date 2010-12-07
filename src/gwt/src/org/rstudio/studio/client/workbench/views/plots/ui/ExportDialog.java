/*
 * ExportDialog.java
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
package org.rstudio.studio.client.workbench.views.plots.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.ExportOptions;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

public class ExportDialog extends ModalDialogBase
{
   public ExportDialog(PlotsServerOperations server,
                       ExportOptions exportOptions,
                       final OperationWithInput<ExportOptions> operation)
   { 
      exportOptions_ = exportOptions;
      server_ = server;
      
      setText("Export Plot as Image");
     
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      
      ThemedButton closeButton = new ThemedButton("Close", new ClickHandler() {
         public void onClick(ClickEvent event) {
            operation.execute(getExportOptions());
            closeDialog();
         }
      });
      addOkButton(closeButton); 
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel verticalPanel = new VerticalPanel();
      verticalPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
      
      // options panel
      optionsPanel_ = new HorizontalPanel();
      optionsPanel_.setWidth("100%");
      optionsPanel_.setStylePrimaryName(RESOURCES.styles().imageOptions());
      optionsPanel_.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      optionsPanel_.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      
      // image size
      HorizontalPanel imageSizePanel = new HorizontalPanel();
      imageSizePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      imageSizePanel.add(createImageOptionLabel("Width:"));
      widthTextBox_ = createImageSizeTextBox();
      setWidth(exportOptions_.getWidth());
      imageSizePanel.add(widthTextBox_);
      widthTextBox_.addChangeHandler(new ChangeHandler() {
         public void onChange(ChangeEvent event)
         {
            updateDownloadHTML();
         }   
      });
      
      imageSizePanel.add(new HTML("&nbsp;&nbsp;"));
      imageSizePanel.add(createImageOptionLabel("Height:"));
      heightTextBox_ = createImageSizeTextBox();
      setHeight(exportOptions_.getHeight());
      imageSizePanel.add(heightTextBox_);
      heightTextBox_.addChangeHandler(new ChangeHandler() {
         public void onChange(ChangeEvent event)
         {
            updateDownloadHTML();
         }
      });
      
      // update button
      ThemedButton updateButton = new ThemedButton("Update Size", 
                                                   new ClickHandler(){
         public void onClick(ClickEvent event) {
            updatePreview(false);
         }
      });
      updateButton.getElement().getStyle().setMarginTop(5, Unit.PX);
      imageSizePanel.add(new HTML("&nbsp;"));
      imageSizePanel.add(updateButton);  
      
      optionsPanel_.add(imageSizePanel);
     
      
      // add options panel
      verticalPanel.add(optionsPanel_);
      
      // image panel
      imagePreview_ = new ImageFrame();
      imagePreview_.setUrl("about:blank");
      imagePreview_.setStylePrimaryName(RESOURCES.styles().imagePreview());
      verticalPanel.add(imagePreview_);

      actionsWidget_ = GWT.create(ActionsWidget.class);
      actionsWidget_.initialize(imagePreview_, server_);
      updateDownloadHTML();

      if (actionsWidget_.shouldPositionOnTopRight())
      {
         optionsPanel_.add(actionsWidget_);
         optionsPanel_.setCellHorizontalAlignment(actionsWidget_,
                                                  HorizontalPanel.ALIGN_RIGHT);
         actionsWidget_.getElement().getStyle().setMarginTop(5, Unit.PX);
      }
      else
      {
         verticalPanel.add(actionsWidget_);
      }

      // update the preview
      updatePreview(true);
         
      // return the panel
      return verticalPanel;
   }

   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      updatePreview(false);
   }
   
  
   private void updateDownloadHTML()
   {
      actionsWidget_.onPlotChanged(getPreviewUrl(true),
                                   getWidth(),
                                   getHeight());
   }

   private void updatePreview(boolean resizeOnly)
   {
      // update size of widgets and center dialog
      if (!resizeOnly)
         imagePreview_.setImageUrl(null);

      String w = Math.min(getWidth(), Window.getClientWidth() - 80) + "px";
      String h = Math.min(getHeight(), Window.getClientHeight() - 250) + "px";
      imagePreview_.setSize(w, h);
      
      if (!resizeOnly)
      {
         center();
         imagePreview_.setImageUrl(getPreviewUrl(false));
      }
   }
  
   private String getPreviewUrl(boolean attachment)
   {
      // get options
      ExportOptions options = getExportOptions();
      
      // determine extension for current type
      String imageType = options.getType();
      String ext = null;
      if (imageType.equals(ExportOptions.PNG_TYPE))
         ext = "png";
      else if (imageType.equals(ExportOptions.JPEG_TYPE))
         ext = "jpg";
      else
         throw new IllegalStateException("Bad image type: " + imageType);
     
      // build preview url
      String previewURL = server_.getGraphicsUrl("plot." + ext);
      previewURL += "?";
      previewURL += "width=" + options.getWidth();
      previewURL += "&";
      previewURL += "height=" + options.getHeight();
      // append random number to default over-aggressive image caching
      // by browsers
      previewURL += "&randomizer=" + Random.nextInt();
      if (attachment)
         previewURL += "&attachment=1";
      
      return previewURL;
   }
   
   private ExportOptions getExportOptions()
   {
      return ExportOptions.create(ExportOptions.PNG_TYPE, 
                                  getWidth(), 
                                  getHeight());
   }
   
   
   // width and height
   private void setWidth(int width)
   {
      lastWidth_ = width;
      widthTextBox_.setText(Integer.toString(width));
   }
   
   private int getWidth()
   {
      try
      {
         return Integer.parseInt(widthTextBox_.getText().trim());
      }
      catch(NumberFormatException ex)
      {
         setWidth(lastWidth_);
         return lastWidth_;
      }
   }
   
   private void setHeight(int height)
   {
      lastHeight_ = height;
      heightTextBox_.setText(Integer.toString(height));
   }
   
   private int getHeight()
   {
      try
      {
         return Integer.parseInt(heightTextBox_.getText().trim());
      }
      catch(NumberFormatException ex)
      {
         setHeight(lastHeight_);
         return lastHeight_;
      }
   } 
   
   private Label createImageOptionLabel(String text)
   {
      Label label = new Label(text);
      label.setStylePrimaryName(RESOURCES.styles().imageOptionLabel());
      return label;
   }
   
   private TextBox createImageSizeTextBox()
   {
      TextBox textBox = new TextBox();
      textBox.setStylePrimaryName(RESOURCES.styles().imageSizeTextBox());
      return textBox;
   }
   
   private final ExportOptions exportOptions_ ;
   private final PlotsServerOperations server_;

   private HorizontalPanel optionsPanel_;
   private int lastWidth_;
   private TextBox widthTextBox_;
   private int lastHeight_ ;
   private TextBox heightTextBox_;
   private ImageFrame imagePreview_;
   private ActionsWidget actionsWidget_;

   
   public static interface Styles extends CssResource
   {
      String mainWidget();
      String imageOptions();
      String imageTypePanel();
      String imageOptionLabel();
      String imageSizeTextBox();
      String imagePreview();
      String actionsPanel();
   }

   public static interface Resources extends ClientBundle
   {
      @Source("ExportDialog.css")
      Styles styles();
   }
   
   public static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
}
