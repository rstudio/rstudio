/*
 * ShinyApplication.java
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
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.Size;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShinyApplication
{
   @Inject
   public ShinyApplication(EventBus eventBus, 
                           final SatelliteManager satelliteManager)
   {
      eventBus.addHandler(ShinyApplicationStatusEvent.TYPE, 
                          new ShinyApplicationStatusEvent.Handler() {
         @Override
         public void onShowShinyApplication(ShinyApplicationStatusEvent event)
         {
            // open the window 
            satelliteManager.openSatellite(ShinyApplicationSatellite.NAME,     
                                           event.getParams(),
                                           new Size(960,1100));   
         }  
      });
   }
}
