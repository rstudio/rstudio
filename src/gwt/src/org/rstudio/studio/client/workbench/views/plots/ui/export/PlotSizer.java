/*
 * ResizableImagePreview.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.core.client.widget.ResizeGripper;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PlotSizer extends Composite 
{  
   public interface Observer
   {
      void onPlotResized(boolean withMouse);
   }
   
   public PlotSizer(int initialWidth, 
                    int initialHeight,
                    boolean keepRatio,
                    PlotsServerOperations server,
                    Observer observer)
   {
      this(initialWidth, initialHeight, keepRatio, null, server, observer);
   }
   
   
   private void configureHorizontalOptionsPanel(HorizontalPanel panel)
   {
      panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
   }
   
   public PlotSizer(int initialWidth, 
                    int initialHeight,
                    boolean keepRatio,
                    Widget extraWidget,
                    PlotsServerOperations server,
                    final Observer observer)
   {
      // alias objects and resources
      server_ = server;
      ExportPlotDialogResources resources = ExportPlotDialogResources.INSTANCE;
           
      // main widget
      VerticalPanel verticalPanel = new VerticalPanel();
      
      // options panel
      HorizontalPanel optionsPanel = new HorizontalPanel();
      optionsPanel.setStylePrimaryName(resources.styles().imageOptions());
      configureHorizontalOptionsPanel(optionsPanel);
      
      // image width
      HorizontalPanel widthAndHeightPanel = new HorizontalPanel();
      configureHorizontalOptionsPanel(widthAndHeightPanel);
      widthAndHeightPanel.add(createImageOptionLabel("Width:"));
      widthTextBox_ = createImageSizeTextBox();
      widthTextBox_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            // screen out programmatic sets
            if (settingDimenensionInProgress_)
               return;
            
            // enforce min size
            int width = constrainWidth(getImageWidth());
           
            // preserve aspect ratio if requested
            if (getKeepRatio())
            {  
               double ratio = (double)lastHeight_ / (double)lastWidth_;
               int height = constrainHeight((int) (ratio * (double)width));
               setHeightTextBox(height);
            }
  
            // set width
            setWidthTextBox(width);
         }
         
      });
      widthAndHeightPanel.add(widthTextBox_);
     
      // image height
      widthAndHeightPanel.add(new HTML("&nbsp;&nbsp;"));
      widthAndHeightPanel.add(createImageOptionLabel("Height:"));
      heightTextBox_ = createImageSizeTextBox();
      heightTextBox_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            // screen out programmatic sets
            if (settingDimenensionInProgress_)
               return;
            
            // enforce min size
            int height = constrainHeight(getImageHeight());
            
            // preserve aspect ratio if requested
            if (getKeepRatio())
            {
               double ratio = (double)lastWidth_ / (double)lastHeight_;
               int width = constrainWidth((int) (ratio * (double)height));
               setWidthTextBox(width);
            }
           
            // always set height
            setHeightTextBox(height);
         }
         
      });
      widthAndHeightPanel.add(heightTextBox_);
      
      // add width and height panel to options panel container
      optionsPanel.add(widthAndHeightPanel);
  
      // lock ratio check box
      optionsPanel.add(new HTML("&nbsp;&nbsp;"));
      keepRatioCheckBox_ = new CheckBox();
      keepRatioCheckBox_.setValue(keepRatio);
      keepRatioCheckBox_.setText("Maintain aspect ratio");
      optionsPanel.add(keepRatioCheckBox_);
      
      // image and sizer in layout panel (create now so we can call
      // setSize in update button click handler)
      final LayoutPanel previewPanel = new LayoutPanel(); 
     
      
      // update button
      ThemedButton updateButton = new ThemedButton("Update Image Size", 
                                                    new ClickHandler(){
         public void onClick(ClickEvent event) 
         {
            previewPanel.setSize((getImageWidth() + IMAGE_INSET) + "px", 
                                (getImageHeight() + IMAGE_INSET) + "px");
            updateImage();
            
            observer.onPlotResized(false);
         }
      });
      updateButton.getElement().getStyle().setMarginTop(5, Unit.PX);
      optionsPanel.add(new HTML("&nbsp;"));
      optionsPanel.add(updateButton);

      verticalPanel.add(optionsPanel); 
       
      // image frame
      imageFrame_ = new ImageFrame();
      imageFrame_.setUrl("about:blank");
      imageFrame_.setSize("100%", "100%");
      imageFrame_.setMarginHeight(0);
      imageFrame_.setMarginWidth(0);
      imageFrame_.setStylePrimaryName(resources.styles().imagePreview());
      previewPanel.add(imageFrame_);
      previewPanel.setWidgetLeftRight(imageFrame_, 0, Unit.PX, IMAGE_INSET, Unit.PX);
      previewPanel.setWidgetTopBottom(imageFrame_, 0, Unit.PX, IMAGE_INSET, Unit.PX);
      previewPanel.getWidgetContainerElement(imageFrame_).getStyle().setOverflow(Overflow.VISIBLE);
      
      // Stops mouse events from being routed to the iframe, which would
      // interfere with resizing
      FlowPanel imageSurface = new FlowPanel();
      imageSurface.setSize("100%", "100%");
      previewPanel.add(imageSurface);
      previewPanel.setWidgetTopBottom(imageSurface, 0, Unit.PX, 0, Unit.PX);
      previewPanel.setWidgetLeftRight(imageSurface, 0, Unit.PX, 0, Unit.PX);
      
      // resize gripper
      ResizeGripper gripper = new ResizeGripper(new ResizeGripper.Observer() 
      {
         @Override
         public void onResizingStarted()
         {    
            int startWidth = getImageWidth();
            int startHeight = getImageHeight();
            
            widthAspectRatio_ = (double)startWidth / (double)startHeight;
            heightAspectRatio_ = (double)startHeight / (double)startWidth;
         }
         
         @Override
         public void onResizing(int xDelta, int yDelta)
         {
            // get start width and height
            int startWidth = getImageWidth();
            int startHeight = getImageHeight();
            
            // calculate new height and width 
            int newWidth = constrainWidth(startWidth + xDelta);
            int newHeight = constrainHeight(startHeight + yDelta);
            
            // preserve aspect ratio if requested
            if (getKeepRatio())
            {
               if (Math.abs(xDelta) > Math.abs(yDelta))
                  newHeight = (int) (heightAspectRatio_ * (double)newWidth);
               else
                  newWidth = (int) (widthAspectRatio_ * (double)newHeight);
            }
            
            // set text boxes
            setWidthTextBox(newWidth);
            setHeightTextBox(newHeight);  
            
            // set image preview size
            previewPanel.setSize(newWidth + IMAGE_INSET + "px", 
                                 newHeight + IMAGE_INSET + "px");
         }

         @Override
         public void onResizingCompleted()
         {
            updateImage();
            observer.onPlotResized(true);
         } 
         
         private double widthAspectRatio_ = 1.0;
         private double heightAspectRatio_ = 1.0;
      });
      
      // layout gripper
      previewPanel.add(gripper);
      previewPanel.setWidgetRightWidth(gripper, 
                                      0, Unit.PX, 
                                      gripper.getImageWidth(), Unit.PX);
      previewPanel.setWidgetBottomHeight(gripper, 
                                        0, Unit.PX, 
                                        gripper.getImageHeight(), Unit.PX);
     
      // constrain dimensions
      initialWidth = constrainWidth(initialWidth);
      initialHeight = constrainHeight(initialHeight);
            
      // initialie text boxes
      setWidthTextBox(initialWidth);
      setHeightTextBox(initialHeight);
 
      // initialize preview
      previewPanel.setSize((initialWidth + IMAGE_INSET) + "px", 
                          (initialHeight + IMAGE_INSET) + "px");
      
      verticalPanel.add(previewPanel);
     
      initWidget(verticalPanel);
     
   }
 
   public void onSizerShown()
   {  
      updateImage();
      FocusHelper.setFocusDeferred(widthTextBox_);
   }
  
  
   public int getImageWidth()
   {
      try
      {
         return Integer.parseInt(widthTextBox_.getText().trim());
      }
      catch(NumberFormatException ex)
      {
         setWidthTextBox(lastWidth_);
         return lastWidth_;
      }
   }
      
   public int getImageHeight()
   {
      try
      {
         return Integer.parseInt(heightTextBox_.getText().trim());
      }
      catch(NumberFormatException ex)
      {
         setHeightTextBox(lastHeight_);
         return lastHeight_;
      }
   } 
   
   public boolean getKeepRatio()
   {
      return keepRatioCheckBox_.getValue();
   }
      
   private void setWidthTextBox(int width)
   {
      settingDimenensionInProgress_ = true;
      lastWidth_ = width;
      widthTextBox_.setText(Integer.toString(width));
      settingDimenensionInProgress_ = false;
   }
   
   
   private void setHeightTextBox(int height)
   {
      settingDimenensionInProgress_ = true;
      lastHeight_ = height;
      heightTextBox_.setText(Integer.toString(height));
      settingDimenensionInProgress_ = false;
   }
   
   private int constrainWidth(int width)
   {
      if (width < MIN_SIZE)
      {
         keepRatioCheckBox_.setValue(false);
         return MIN_SIZE;
      }
      else
      {
         return width;
      }
   }
   
   private int constrainHeight(int height)
   {
      if (height < MIN_SIZE)
      {
         keepRatioCheckBox_.setValue(false);
         return MIN_SIZE;
      }
      else
      {
         return height;
      }
   }
   
   private Label createImageOptionLabel(String text)
   {
      Label label = new Label(text);
      label.setStylePrimaryName(
            ExportPlotDialogResources.INSTANCE.styles().imageOptionLabel());
      return label;
   }
   
   private TextBox createImageSizeTextBox()
   {
      TextBox textBox = new TextBox();
      textBox.setStylePrimaryName(
            ExportPlotDialogResources.INSTANCE.styles().imageSizeTextBox());
      return textBox;
   }
  
   
   
   private void updateImage()
   {
      imageFrame_.setImageUrl(server_.getPlotExportUrl("png", 
                                                       getImageWidth(),
                                                       getImageHeight(),
                                                       false));
   }
   
   private static final int IMAGE_INSET = 6;
   
   private final ImageFrame imageFrame_;
   private final TextBox widthTextBox_;
   private final TextBox heightTextBox_;
   private final CheckBox keepRatioCheckBox_;
   
   private final PlotsServerOperations server_;
   
   private int lastWidth_;
   private int lastHeight_ ;
  
   private boolean settingDimenensionInProgress_ = false;
   
   private final int MIN_SIZE = 100;
}
