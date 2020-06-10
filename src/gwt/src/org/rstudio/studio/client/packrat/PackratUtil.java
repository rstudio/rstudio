/*
 * PackratUtil.java
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

package org.rstudio.studio.client.packrat;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.inject.Inject;

public class PackratUtil
{
   @Inject
   public PackratUtil(EventBus eventBus,
                      Session session,
                      WorkbenchContext workbenchContext) {
      
      eventBus_ = eventBus;
      session_ = session;
      workbenchContext_ = workbenchContext;
   }
   
   public void executePackratFunction(String name)
   {
      executePackratFunction(name, "");
   }
   
   public void executePackratFunction(String name, String args)
   { 
      // append to args
      String projectArg = packratProjectArg();
      if (projectArg.length() > 0)
      {
         if (args.length() == 0)
            args = projectArg;
         else
            args = args + ", " + projectArg;
      }
      
      String cmd = "packrat::" + name + "(" + args + ")";
      
      eventBus_.fireEvent(new SendToConsoleEvent(cmd, 
                                                 true, 
                                                 true));
   }
   
   public String packratProjectArg()
   {
      String projectArg = "";
      FileSystemItem projectDir = session_.getSessionInfo()
                                       .getActiveProjectDir();
      FileSystemItem workingDir = workbenchContext_
                                       .getCurrentWorkingDir();
      if (!projectDir.equalTo(workingDir))
         projectArg = "project = '" + projectDir.getPath() + "'";
      return projectArg;
   }

   private EventBus eventBus_;
   private Session session_;
   private WorkbenchContext workbenchContext_;
}
