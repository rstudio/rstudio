/*
 * BuildTab.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;


public class BuildTab extends DelayLoadWorkbenchTab<Build>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}
   
   public abstract static class Shim extends DelayLoadTabShim<Build, BuildTab> 
   {
      @Handler
      public abstract void onBuildAll();
      @Handler
      public abstract void onCheckPackage();
   }

   @Inject
   public BuildTab(Shim shim, 
                   final Session session, 
                   Binder binder, 
                   final Commands commands,
                   EventBus eventBus)
   {
      super("Build", shim);
      session_ = session;
      binder.bind(commands, shim);
      
      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         public void onSessionInit(SessionInitEvent sie)
         {
            SessionInfo sessionInfo = session.getSessionInfo();
            
            String type = sessionInfo.getBuildToolsType();
                
            // adapt or remove package-specific commands if this isn't a package
            if (!type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
            {
               commands.checkPackage().remove();
               commands.buildAll().setMenuLabel("_Build All");
            }
            
            // remove all other commands if there are no build tools
            if (type.equals(SessionInfo.BUILD_TOOLS_NONE))
            {
               commands.buildAll().remove();
               commands.activateBuild().remove();
            }
            
           
         }
      });
   }
   
   @Override
   public boolean isSuppressed()
   {
      return session_.getSessionInfo().getBuildToolsType().equals("None");
   }

   private Session session_;
}
