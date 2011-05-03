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
      server_ = server;
      observer_ = observer;
      
      // alias resources
      ExportPlotDialogResources resources = ExportPlotDialogResources.INSTANCE;
      
      // main widget
      VerticalPanel verticalPanel = new VerticalPanel();
      
      // options panel
      HorizontalPanel sizeInputPanel = new HorizontalPanel();
      sizeInputPanel.setStylePrimaryName(resources.styles().imageOptions());
      sizeInputPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      sizeInputPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
      
      // image width
      sizeInputPanel.add(createImageOptionLabel("Width:"));
      widthTextBox_ = createImageSizeTextBox();
      setWidthTextBox(initialWidth);
      sizeInputPanel.add(widthTextBox_);
     
      // image height
      sizeInputPanel.add(new HTML("&nbsp;&nbsp;"));
      sizeInputPanel.add(createImageOptionLabel("Height:"));
      heightTextBox_ = createImageSizeTextBox();
      setHeightTextBox(initialHeight);
      sizeInputPanel.add(heightTextBox_);
  
      // lock ratio check box
      sizeInputPanel.add(new HTML("&nbsp;&nbsp;"));
      keepRatioCheckBox_ = new CheckBox();
      keepRatioCheckBox_.setValue(keepRatio);
      keepRatioCheckBox_.setText("Keep ratio");
      sizeInputPanel.add(keepRatioCheckBox_);
      
      // image and sizer in layout panel (create now so we can call
      // setSize in update button click handler)
      final LayoutPanel layoutPanel = new LayoutPanel(); 
     
      
      // update button
      ThemedButton previewButton = new ThemedButton("Preview", 
                                                    new ClickHandler(){
         public void onClick(ClickEvent event) 
         {
            layoutPanel.setSize((getImageWidth() + IMAGE_INSET) + "px", 
                                (getImageHeight() + IMAGE_INSET) + "px");
            updateImage();
            
            observer_.onPlotResized(false);
         }
      });
      previewButton.getElement().getStyle().setMarginTop(5, Unit.PX);
      sizeInputPanel.add(new HTML("&nbsp;"));
      sizeInputPanel.add(previewButton);

      verticalPanel.add(sizeInputPanel); 
       
      // image frame
      imageFrame_ = new ImageFrame();
      imageFrame_.setUrl("about:blank");
      imageFrame_.setSize("100%", "100%");
      imageFrame_.setMarginHeight(0);
      imageFrame_.setMarginWidth(0);
      imageFrame_.setStylePrimaryName(resources.styles().imagePreview());
      layoutPanel.add(imageFrame_);
      layoutPanel.setWidgetLeftRight(imageFrame_, 0, Unit.PX, IMAGE_INSET, Unit.PX);
      layoutPanel.setWidgetTopBottom(imageFrame_, 0, Unit.PX, IMAGE_INSET, Unit.PX);
      layoutPanel.getWidgetContainerElement(imageFrame_).getStyle().setOverflow(Overflow.VISIBLE);
      
      // Stops mouse events from being routed to the iframe, which would
      // interfere with resizing
      FlowPanel imageSurface = new FlowPanel();
      imageSurface.setSize("100%", "100%");
      layoutPanel.add(imageSurface);
      layoutPanel.setWidgetTopBottom(imageSurface, 0, Unit.PX, 0, Unit.PX);
      layoutPanel.setWidgetLeftRight(imageSurface, 0, Unit.PX, 0, Unit.PX);
      
      // resize gripper
      ResizeGripper gripper = new ResizeGripper(new ResizeGripper.Observer() 
      {
         @Override
         public void onResizingStarted()
         {
            
         }
         
         @Override
         public void onResizing(int xDelta, int yDelta)
         {
            // get start width and height
            int startWidth = getImageWidth();
            int startHeight = getImageHeight();
            
            // calculate new height and width 
            int newWidth = startWidth + xDelta;
            int newHeight = startHeight + yDelta;
           
            
            setWidthTextBox(newWidth);
            setHeightTextBox(newHeight);
            
            layoutPanel.setSize(newWidth + IMAGE_INSET + "px", 
                                newHeight + IMAGE_INSET + "px");
         }

         @Override
         public void onResizingCompleted()
         {
            updateImage();
            
            observer_.onPlotResized(true);
         }     
      });
      
      // layout gripper
      layoutPanel.add(gripper);
      layoutPanel.setWidgetRightWidth(gripper, 
                                      0, Unit.PX, 
                                      gripper.getImageWidth(), Unit.PX);
      layoutPanel.setWidgetBottomHeight(gripper, 
                                        0, Unit.PX, 
                                        gripper.getImageHeight(), Unit.PX);
     
      
      layoutPanel.setSize((initialWidth + IMAGE_INSET) + "px", 
                          (initialHeight + IMAGE_INSET) + "px");
      
      verticalPanel.add(layoutPanel);
     
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
      lastWidth_ = width;
      widthTextBox_.setText(Integer.toString(width));
   }
   
   
   private void setHeightTextBox(int height)
   {
      lastHeight_ = height;
      heightTextBox_.setText(Integer.toString(height));
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
   private final Observer observer_;
   
   private int lastWidth_;
   private int lastHeight_ ;
}
