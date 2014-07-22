/*
 * ViewerPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigatedEvent;

public class ViewerPane extends WorkbenchPane implements ViewerPresenter.Display
{
   @Inject
   public ViewerPane(Commands commands, GlobalDisplay globalDisplay, EventBus events)
   {
      super("Viewer");
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      events_ = events;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      // add zoom button
      toolbar.addLeftWidget(commands_.viewerZoom().createToolbarButton());
      
      // add publish button 
      publishButtonSeparator_ = toolbar.addLeftSeparator();
      publishButton_ = commands_.publishHTML().createToolbarButton(false);
      toolbar.addLeftWidget(publishButton_);
      publishButtonSeparator_.setVisible(false);
      publishButton_.setVisible(false);
      
      toolbar.addRightWidget(commands_.viewerClear().createToolbarButton());
      toolbar.addRightWidget(commands_.viewerStop().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.viewerPopout().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.viewerRefresh().createToolbarButton());
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame() ;
      frame_.setSize("100%", "100%");
      navigate(ABOUT_BLANK);
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void navigate(String url)
   {
      navigate(url, false);
      publishButton_.setVisible(false);
      publishButtonSeparator_.setVisible(false);
      rmdPreviewParams_ = null;
   }

   @Override
   public void previewRmd(RmdPreviewParams params)
   {
      navigate(params.getOutputUrl(), true);
      publishButton_.setVisible(!params.isShinyDocument());
      publishButtonSeparator_.setVisible(!params.isShinyDocument());
      if (!params.isShinyDocument())
         publishButton_.setText(params.getResult().getRpubsPublished() ? 
               "Republish" : "Publish");
      rmdPreviewParams_ = params;
   }
   
   @Override
   public String getUrl()
   {
      return frame_.getUrl();
   }
   
   @Override
   public String getTitle()
   {
      return frame_.getTitle();
   }
   
   @Override
   public void popout()
   {
      if (rmdPreviewParams_ != null && 
          !rmdPreviewParams_.isShinyDocument())
      {
         globalDisplay_.showHtmlFile(rmdPreviewParams_.getOutputFile());
      }
      else if (unmodifiedUrl_ != null)
      {
         globalDisplay_.openWindow(unmodifiedUrl_);
      }
   }

   @Override
   public void refresh()
   {
      String url = frame_.getUrl();
      if (url != null)
         frame_.setUrl(url);
   }
   
   @Override
   public Size getViewerFrameSize()
   {
      return new Size(frame_.getOffsetWidth(), frame_.getOffsetHeight());
   }
   
   private void navigate(String url, boolean useRawURL)
   {
      // save the unmodified URL for pop-out
      unmodifiedUrl_ = url;
      
      // append the viewer_pane query parameter
      if ((unmodifiedUrl_ != null) && 
          !unmodifiedUrl_.equals(ABOUT_BLANK) &&
          !useRawURL)
      {
         // first split into base and anchor
         String base = new String(unmodifiedUrl_);
         String anchor = new String();
         int anchorPos = base.indexOf('#');
         if (anchorPos != -1)
         {
            anchor = base.substring(anchorPos);
            base = base.substring(0, anchorPos);
         }
         
         // add the query param
         if (!base.contains("?"))
            base = base + "?";
         else
            base = base + "&";
         base = base + "viewer_pane=1";
        
         // add the anchor back on
         String viewerUrl = base + anchor;
         
         // set the url
         frame_.setUrl(viewerUrl);
      }
      else
      {
         frame_.setUrl(unmodifiedUrl_);
      }
      
      events_.fireEvent(new ViewerNavigatedEvent(url, frame_));
   }

   private RStudioFrame frame_;
   private String unmodifiedUrl_;
   private RmdPreviewParams rmdPreviewParams_;
   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   
   private ToolbarButton publishButton_;
   private Widget publishButtonSeparator_;

   public static final String ABOUT_BLANK = "about:blank";
}
