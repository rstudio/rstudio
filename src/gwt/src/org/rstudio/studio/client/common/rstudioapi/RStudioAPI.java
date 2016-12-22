/*
 * RStudioAPI.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.common.rstudioapi;

import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rstudioapi.events.RStudioAPIShowDialogEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RStudioAPI implements RStudioAPIShowDialogEvent.Handler
{
   public RStudioAPI()
   {
   }

   @Inject
   private void initialize(EventBus events,
                           GlobalDisplay globalDisplay)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;

      events_.addHandler(RStudioAPIShowDialogEvent.TYPE, this);
   }

   @Override
   public void onRStudioAPIShowDialogEvent(RStudioAPIShowDialogEvent event)
   {
      globalDisplay_.showMessage(
               MessageDialog.INFO, 
               event.getTitle(), 
               event.getMessage());
   }

   private EventBus events_;
   private GlobalDisplay globalDisplay_;
}
