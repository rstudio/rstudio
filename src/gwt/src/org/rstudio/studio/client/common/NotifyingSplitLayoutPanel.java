/*
 * NotifyingSplitLayoutPanel.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.application.events.EventBus;

public class NotifyingSplitLayoutPanel extends SplitLayoutPanel
{
   @Inject
   public NotifyingSplitLayoutPanel(int splitterSize, EventBus events)
   {
      super(splitterSize);

      events_ = events;

      addSplitterBeforeResizeHandler(new SplitterBeforeResizeHandler()
      {
         public void onSplitterBeforeResize(SplitterBeforeResizeEvent event)
         {
            events_.fireEvent(new GlassVisibilityEvent(true));
         }
      });

      addSplitterResizedHandler(new SplitterResizedHandler()
      {
         public void onSplitterResized(SplitterResizedEvent event)
         {
            events_.fireEvent(new GlassVisibilityEvent(false));
         }
      });
   }

   private final EventBus events_;
}
