/*
 * ViewerPanePreviewer.java
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

package org.rstudio.studio.client.workbench.views.viewer.export;

import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotPreviewer;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;

import com.google.gwt.user.client.ui.Widget;

public class ViewerPanePreviewer implements ExportPlotPreviewer
{
   public ViewerPanePreviewer(String url)
   {
      url_ = URIUtils.addQueryParam(url, "viewer_export", "1");
   }
   
   @Override
   public boolean getLimitToScreen()
   {
      return true;
   }
   
   @Override
   public Widget getWidget()
   {
      if (frame_ == null)
      {
         frame_ = new RStudioFrame("Viewer Pane Preview");
         frame_.setUrl(url_);
         frame_.setSize("100%", "100%");
         frame_.setStylePrimaryName(
               ExportPlotResources.INSTANCE.styles().imagePreview());
      }
      
      return frame_;
   }

   @Override
   public IFrameElementEx getPreviewIFrame()
   {
      return frame_.getElement().<IFrameElementEx>cast();
   }
   
   @Override
   public void updatePreview(int width, int height)
   {
   }
   
   private RStudioFrame frame_ = null;
   
   protected final String url_;
}
