/*
 * SourceWindowManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.Size;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceWindowParams;

import com.google.inject.Inject;

public class SourceWindowManager implements PopoutDocEvent.Handler
{
   public SourceWindowManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(SatelliteManager satelliteManager, EventBus events)
   {
      events_ = events;
      satelliteManager_ = satelliteManager;
      events_.addHandler(PopoutDocEvent.TYPE, this);
   }

   @Override
   public void onPopoutDoc(PopoutDocEvent evt)
   {
      String windowId = createSourceWindowId();
      SourceWindowParams params = SourceWindowParams.create(windowId, 
            evt.getId());
      satelliteManager_.openSatellite(
            SourceSatellite.NAME_PREFIX + windowId, params, 
            new Size(500, 800));
   }
   
   private String createSourceWindowId()
   {
      String alphanum = "0123456789abcdefghijklmnopqrstuvwxyz";
      String id = "";
      for (int i = 0; i < 12; i++)
      {
         id += alphanum.charAt((int)(Math.random() * alphanum.length()));
      }
      return id;
   }
   
   private EventBus events_;
   private SatelliteManager satelliteManager_;
}
