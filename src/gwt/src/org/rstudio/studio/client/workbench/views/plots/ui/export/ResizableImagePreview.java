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

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;

public class ResizableImagePreview extends Composite
{
   public ResizableImagePreview()
   {
      // alias resources
      ExportPlotDialogResources resources = ExportPlotDialogResources.INSTANCE;
      
      // main widget
      LayoutPanel layoutPanel = new LayoutPanel();
      
      // image frame
      imageFrame_ = new ImageFrame();
      imageFrame_.setSize("100%", "100%");
      imageFrame_.setUrl("about:blank");
      imageFrame_.setStylePrimaryName(resources.styles().imagePreview());
      layoutPanel.add(imageFrame_);
      layoutPanel.setWidgetLeftRight(imageFrame_, 0, Unit.PX, 8, Unit.PX);
      layoutPanel.setWidgetTopBottom(imageFrame_, 0, Unit.PX, 8, Unit.PX);
      layoutPanel.getWidgetContainerElement(imageFrame_).getStyle().setOverflow(Overflow.VISIBLE);
      
      // resize gripper
      ResizeGripper gripper = new ResizeGripper();
      layoutPanel.add(gripper);
      layoutPanel.setWidgetRightWidth(gripper, 
                                      0, Unit.PX, 
                                      gripper.getImageWidth(), Unit.PX);
      layoutPanel.setWidgetBottomHeight(gripper, 
                                        0, Unit.PX, 
                                        gripper.getImageHeight(), Unit.PX);
     
      
     
      initWidget(layoutPanel);
   }
   
   
   
   
   
   private ImageFrame imageFrame_;
}
