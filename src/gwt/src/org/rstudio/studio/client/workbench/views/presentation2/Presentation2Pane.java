/*
 * Presentation2Pane.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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



package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.widget.AnchorableFrame;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Presentation2Pane extends WorkbenchPane implements Presentation2.Display
{
   @Inject
   public Presentation2Pane()
   {
      super("Presentation");
      ensureWidget();
   }
   

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Presentation Toolbar");
      
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_PRESENTATION2,
            RSConnect.CONTENT_TYPE_NONE, true, null);
      toolbar.addRightWidget(publishButton_);
      
      /*
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.viewerRefresh().createToolbarButton());
      */
      
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new AnchorableFrame("Presentation Preview");
      frame_.setSize("100%", "100%");
      frame_.setUrl(URIConstants.ABOUT_BLANK);
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void showPresentation(String url, QuartoNavigate nav)
   {
      // bring tab to front
      bringToFront();
      
      // compute min height based on reveal presentation ratio and current width
      int frameWidth = frame_.getOffsetWidth();
      if (frameWidth > 0)
      {
         double ratio = (double)700 / 960;
         ensureHeight((int)(frameWidth * ratio));
      }
     
      // navigate
      frame_.setUrl(url);
      
      // set publish button
      if (nav.isWebsite())
         publishButton_.setQuartoSitePreview();
      else
         publishButton_.setQuartoDocPreview(nav.getSourceFile(), nav.getOutputFile());
   }


   
   private RStudioFrame frame_;
   private RSConnectPublishButton publishButton_;

}
