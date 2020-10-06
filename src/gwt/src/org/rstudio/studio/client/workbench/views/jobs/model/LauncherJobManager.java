/*
 * LauncherJobManager.java
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

package org.rstudio.studio.client.workbench.views.jobs.model;

import org.rstudio.core.client.SessionServer;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class LauncherJobManager
{
   public void startTrackingAllJobStatuses()
   {
   }
   
   public void stopTrackingAllJobStatuses()
   {
   }
   
   public void controlLauncherJob(String jobId, String operation, ServerRequestCallback<Boolean> callback)
   {
   }

   public void reset(SessionServer server)
   {
      // resets the state of this launcher object and its dependencies
      // in preparation for connecting to a different launcher server
   }

   public SessionServer getSessionServer()
   {
      // return the currently active Jobs session server
      // or null if not in effect
      return null;
   }
}
