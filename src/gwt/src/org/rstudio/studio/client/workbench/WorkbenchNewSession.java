/*
 * WorkbenchNewSession.java
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

package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.ProjectOpener;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

public class WorkbenchNewSession
{    
   public void openNewSession(GlobalDisplay globalDisplay,
                              WorkbenchContext workbenchContext,
                              final WorkbenchServerOperations serverOperations,
                              ProjectOpener projectOpener,
                              ApplicationServerOperations server)
   {
      String project = workbenchContext.getActiveProjectFile();
      if (project != null)
      {
         Desktop.getFrame().openProjectInNewWindow(project); 
      }
      else
      {
         Desktop.getFrame().openSessionInNewWindow(
               StringUtil.notNull(workbenchContext.getCurrentWorkingDir().getPath()));
      }
   }
}
