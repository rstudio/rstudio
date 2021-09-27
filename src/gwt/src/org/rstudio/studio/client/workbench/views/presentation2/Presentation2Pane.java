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
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Presentation2Pane extends WorkbenchPane implements Presentation2.Display
{
   @Inject
   public Presentation2Pane(Commands commands)
   {
      super("Presentation");
      commands_ = commands;
      ensureWidget();
   }
   

   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar("Presentation Toolbar");
      
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_PRESENTATION2,
            RSConnect.CONTENT_TYPE_NONE, true, null);
      toolbar_.addRightWidget(publishButton_);
   
      toolbar_.addRightSeparator();
      toolbar_.addRightWidget(commands_.refreshPresentation2().createToolbarButton());
      
      return toolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame("Presentation Preview");
      frame_.addStyleName("ace_editor_theme");
      frame_.setSize("100%", "100%");
      frame_.setUrl(URIConstants.ABOUT_BLANK);
      frame_.getElement().setAttribute("webkitallowfullscreen", "");
      frame_.getElement().setAttribute("mozallowfullscreen", "");
      frame_.getElement().setAttribute("allowfullscreen", "");
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void showPresentation(String url, QuartoNavigate nav)
   {
      // bring tab to front
      bringToFront();
      
      // in desktop mode we need to be careful about loading URLs which are
      // non-local; before changing the URL, set the iframe to be sandboxed
      // based on whether we're working with a local URL (note that prior to
      // RStudio 1.2 local URLs were forbidden entirely)
      if (Desktop.hasDesktopFrame())
      {
         if (URIUtils.isLocalUrl(url))
         {
            frame_.getElement().removeAttribute("sandbox");
         }
         else
         {
            frame_.getElement().setAttribute("sandbox", "allow-scripts");
         }
      }

      
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
      
      // redraw toolbar
      toolbar_.invalidateSeparators();
      
      
   }
   
   @Override
   public void refresh()
   {
      try
      {
         frame_.getWindow().reload();
      }
      catch (Exception e)
      {
         String url = frame_.getUrl();
         if (url != null)
            frame_.setUrl(url);
      }
   }

   
   private RStudioFrame frame_;
   private Toolbar toolbar_;
   private RSConnectPublishButton publishButton_;
   
   private final Commands commands_;
   

}
