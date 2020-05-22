/*
 * PlotsPanePreviewer.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.plots.ui.export;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ImageFrame;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.Widget;

public class PlotsPanePreviewer implements ExportPlotPreviewer
{
   public PlotsPanePreviewer(PlotsServerOperations server)
   {
      this(server, false);
   }
   
   public PlotsPanePreviewer(PlotsServerOperations server,
                             boolean limitToScreen)
   {
      server_ = server;
      limitToScreen_ = limitToScreen;
   }
   
   @Override
   public boolean getLimitToScreen()
   {
      return limitToScreen_;
   }
   
   @Override
   public Widget getWidget()
   {
      if (imageFrame_ == null)
      {
         imageFrame_ = new ImageFrame("Plot Preview");
         imageFrame_.setUrl("about:blank");
         imageFrame_.setSize("100%", "100%");
         imageFrame_.setMarginHeight(0);
         imageFrame_.setMarginWidth(0);
         imageFrame_.setStylePrimaryName(
               ExportPlotResources.INSTANCE.styles().imagePreview());
      }
      return imageFrame_;
   }

   @Override
   public IFrameElementEx getPreviewIFrame()
   {
      return imageFrame_.getElement().<IFrameElementEx>cast();
   }
   

   public Rectangle getPreviewClientRect()
   {
      WindowEx win = imageFrame_.getElement().<IFrameElementEx>cast()
            .getContentWindow();
      Document doc = win.getDocument();
      NodeList<Element> images = doc.getElementsByTagName("img");
      if (images.getLength() > 0)
      {
         ElementEx img = images.getItem(0).cast();
         return new Rectangle(img.getClientLeft(),
                              img.getClientTop(),
                              img.getClientWidth(),
                              img.getClientHeight());
      }
      else
      {
         return new Rectangle(0,0,0,0);
      }
   }
   
   @Override
   public void updatePreview(int width, int height)
   {
      imageFrame_.setImageUrl(server_.getPlotExportUrl("png", 
                                                       width,
                                                       height,
                                                       false));
      
     
   }
   
   private ImageFrame imageFrame_ = null;
   
   protected final PlotsServerOperations server_;
   private final boolean limitToScreen_;
}
