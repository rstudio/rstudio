/*
 * RmdOutput.java
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
package org.rstudio.studio.client.rmarkdown;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderCompletedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RmdOutput implements RmdRenderCompletedEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, RmdOutput> {}

   @Inject
   public RmdOutput(EventBus eventBus, 
                    Commands commands,
                    Binder binder,
                    final SatelliteManager satelliteManager)
   {
      satelliteManager_ = satelliteManager;
      
      eventBus.addHandler(RmdRenderCompletedEvent.TYPE, this);

      binder.bind(commands, this);
   }
   
   @Override
   public void onRmdRenderCompleted(RmdRenderCompletedEvent event)
   {
      if (event.getResult().getSucceeded())
      {
         satelliteManager_.openSatellite(RmdOutputSatellite.NAME,     
                                         event.getResult(),
                                         new Size(960,1100));   
      }
   }
   
   private final SatelliteManager satelliteManager_;
}