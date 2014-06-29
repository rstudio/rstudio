/*
 * RmdOutputFramePane.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigateEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigatedEvent;

import com.google.inject.Inject;

public class RmdOutputFramePane extends RmdOutputFrameBase 
                                implements ViewerNavigatedEvent.Handler
{
   @Inject
   public RmdOutputFramePane(EventBus events)
   {
      events_ = events;
      events.addHandler(ViewerNavigatedEvent.TYPE, this);
   }

   @Override
   public void closeOutputFrame()
   {
      if (frame_ != null)
         frame_.setUrl("about:blank");
   }

   @Override
   public WindowEx getWindowObject()
   {
     if (frame_ == null)
        return null;
     return frame_.getWindow();
   }

   @Override
   public void showRmdPreview(RmdPreviewParams params)
   {
      super.showRmdPreview(params);
      ViewerNavigateEvent.Data data = ViewerNavigateEvent.Data.create(
            params.getOutputUrl(), params.getPreferredSize().height, 
            true);
      events_.fireEvent(new ViewerNavigateEvent(data));
   }

   @Override
   public void onViewerNavigated(ViewerNavigatedEvent event)
   {
      if (outputUrl_.equals(event.getURL()))
      {
         frame_ = event.getFrame();
      }
      else
      {
         frame_ = null;
      }
   }
   
   @Override
   public int getViewerType()
   {
      return RmdOutput.RMD_VIEWER_TYPE_PANE;
   }
   
   private String outputUrl_;
   private RStudioFrame frame_;
   private final EventBus events_;
}
