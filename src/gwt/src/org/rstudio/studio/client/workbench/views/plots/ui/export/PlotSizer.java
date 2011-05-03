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

import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.core.client.widget.ResizeGripper;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;

public class PlotSizer extends Composite 
{  
   public PlotSizer(int initialWidth, 
                    int initialHeight,
                    PlotsServerOperations server)
   {
      server_ = server;
      
      // alias resources
      ExportPlotDialogResources resources = ExportPlotDialogResources.INSTANCE;
      
      // main widget
      LayoutPanel layoutPanel = new LayoutPanel();
     
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
      
      // resize gripper
      ResizeGripper gripper = new ResizeGripper(new ResizeGripper.Observer() 
      {
         @Override
         public void onResizing(int xDelta, int yDelta)
         {
            setSize(getOffsetWidth() + xDelta + "px",
                    getOffsetHeight() + yDelta + "px");
         }

         @Override
         public void onResizingCompleted()
         {
            updateImage();
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
     
      
     
      initWidget(layoutPanel);
      
      setSize((initialWidth + IMAGE_INSET) + "px", 
              (initialHeight + IMAGE_INSET) + "px");

   }
 
   public void loadInitialImage()
   {
      updateImage();
   }
  
   public int getImageWidth()
   {
      return imageFrame_.getOffsetWidth();
   }
   
   public int getImageHeight()
   {
      return imageFrame_.getOffsetHeight();
   }
   
   
   private void updateImage()
   {
      imageFrame_.setImageUrl(server_.getPlotExportUrl("png", 
                              getImageWidth(),
                              getImageHeight(),
                              false));
   }
   
   
   private final ImageFrame imageFrame_;
   private final PlotsServerOperations server_;
   private final int IMAGE_INSET = 6;
}
