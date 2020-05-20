/*
 * RmdOutputFramePane.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.core.client.ScrollUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.ShinyFrameHelper;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerClearedEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigatedEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerPreviewRmdEvent;

import com.google.inject.Inject;

public class RmdOutputFramePane extends RmdOutputFrameBase 
                                implements ViewerNavigatedEvent.Handler,
                                           ViewerClearedEvent.Handler
{
   @Inject
   public RmdOutputFramePane(EventBus events, 
                             RMarkdownServerOperations server)
   {
      events_ = events;
      server_ = server;
      events.addHandler(ViewerNavigatedEvent.TYPE, this);
      events.addHandler(ViewerClearedEvent.TYPE, this);
      shinyFrame_ = new ShinyFrameHelper();
   }

   @Override
   public void closeOutputFrame(boolean forReopen)
   {
      events_.fireEvent(new ViewerClearedEvent(false));
   }

   @Override
   public WindowEx getWindowObject()
   {
      if (frame_ == null)
         return null;
      return frame_.getWindow();
   }

   @Override
   public void showRmdPreview(RmdPreviewParams params, boolean activate)
   {
      super.showRmdPreview(params);
      isShiny_ = params.isShinyDocument();
      
      events_.fireEvent(new ViewerPreviewRmdEvent(params, frame_ != null));
   }

   @Override
   public void onViewerNavigated(ViewerNavigatedEvent event)
   {
      final RmdPreviewParams params = getPreviewParams();
      if (params != null &&
          params.getOutputUrl() == event.getURL())
      {
         frame_ = event.getFrame();
         if (params.isShinyDocument())
         {
            shinyFrame_.initialize(
               DomUtils.makeAbsoluteUrl(params.getOutputUrl()),
               new Operation() {
                  @Override
                  public void execute()
                  {
                     shinyFrame_.setScrollPosition(
                           params.getScrollPosition());
                  }
               });
         }
         else
         {
            ScrollUtil.setScrollPositionOnLoad(
                  frame_, getPreviewParams().getScrollPosition());
         }
      }
      else
      {
         frame_ = null;
      }
   }
   
   @Override
   public void onViewerCleared(ViewerClearedEvent event)
   {
      // if a Shiny document is running in the viewer, stop render when the
      // viewer is cleared
      if (isShiny_ && event.isForStop())
      {
         server_.terminateRenderRmd(true, new VoidServerRequestCallback());
         isShiny_ = false;
      }
   }

   @Override
   public String getViewerType()
   {
      return UserPrefs.RMD_VIEWER_TYPE_PANE;
   }
   
   @Override
   public int getScrollPosition()
   {
      if (isShiny_ && shinyFrame_ != null)
      {
         return shinyFrame_.getScrollPosition();
      }
      else
      {
         int position = 0;
         try
         {
            position = frame_.getIFrame().getContentWindow().getScrollTop();
         }
         catch(Exception ex)
         {
            // can happen if there's no document at this point, or the browser
            // thinks we're a different origin; handle this by returning a safe
            // default below
         }

         return position;
      }
   }
   
   @Override
   public String getAnchor()
   {
      String url = null;
      if (isShiny_ && shinyFrame_ != null)
      {
         url = shinyFrame_.getUrl();
      }
      else
      {
         try
         {
            url = frame_.getIFrame().getContentDocument().getURL();
         }
         catch(Exception x)
         {
            // can happen if there's no document at this point, or the browser
            // thinks we're a different origin; handle this by returning a safe
            // default below
         }
      }
      if (url == null)
         return "";
      int anchorPos = url.lastIndexOf("#");
      return anchorPos > 0 ? url.substring(anchorPos + 1) : "";
   }
   
   private RStudioFrame frame_;
   private ShinyFrameHelper shinyFrame_;
   private boolean isShiny_;
   
   private final RMarkdownServerOperations server_;
   private final EventBus events_;
}
