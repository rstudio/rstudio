/*
 * WindowCloseMonitor.java
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
package org.rstudio.core.client.dom;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.satellite.SatelliteManager;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

public class WindowCloseMonitor
{
   // When a window is unloaded; it could be that the window is unloading for
   // refresh or closing for good. To distinguish between the two cases, we ping
   // the window for 5 seconds after receiving the unload.
   public static void monitorSatelliteClosure(final String windowName,
         final Command onClosed, final Command onOpen)
   {
      final SatelliteManager satelliteManager = RStudioGinjector.INSTANCE
                             .getSatelliteManager();
      final WindowEx window = satelliteManager.getSatelliteWindowObject(
                                    windowName);
      Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            if (window == null ||
                window.isClosed() ||
                satelliteManager.getSatelliteWindowObject(
                      windowName) == null)
            {
               onClosed.execute();
               return false;
            }
            // retry up to 5 seconds (250ms per try)
            if (retries_++ < 20)
            {
               return true;
            }
            else
            {
               if (onOpen != null)
                  onOpen.execute();
               return false;
            }
         }

         private int retries_ = 0;

      }, 250);
   }
}
