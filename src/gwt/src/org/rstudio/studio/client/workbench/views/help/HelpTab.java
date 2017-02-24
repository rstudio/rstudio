/*
 * HelpTab.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.help;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.help.events.ActivateHelpEvent;
import org.rstudio.studio.client.workbench.views.help.events.ActivateHelpHandler;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpHandler;

public class HelpTab extends DelayLoadWorkbenchTab<Help>
{
   public abstract static class Shim extends DelayLoadTabShim<Help, HelpTab>
                                     implements ShowHelpHandler,
                                                ActivateHelpHandler
   {
      @Handler public abstract void onHelpHome();
      @Handler public abstract void onDebugHelp();
      @Handler public abstract void onMarkdownHelp();
      @Handler public abstract void onOpenRStudioIDECheatSheet();
      @Handler public abstract void onOpenDataVisualizationCheatSheet();
      @Handler public abstract void onOpenDataImportCheatSheet();
      @Handler public abstract void onOpenPackageDevelopmentCheatSheet();
      @Handler public abstract void onOpenDataWranglingCheatSheet();
      @Handler public abstract void onOpenDataTransformationCheatSheet();
      @Handler public abstract void onOpenRMarkdownCheatSheet();
      @Handler public abstract void onOpenRMarkdownReferenceGuide();
      @Handler public abstract void onOpenShinyCheatSheet();
      @Handler public abstract void onOpenSparklyrCheatSheet();
      @Handler public abstract void onOpenRoxygenQuickReference();
      @Handler public abstract void onProfileHelp();
      
      public abstract void bringToFront();
   }
   
   public interface Binder extends CommandBinder<Commands, HelpTab.Shim> {}

   @Inject
   public HelpTab(final Shim shim,
                  Binder binder,
                  Commands commands,
                  EventBus events,
                  final Session session)
   {
      super("Help", shim);
      binder.bind(commands, shim);
      events.addHandler(ShowHelpEvent.TYPE, shim);
      events.addHandler(ActivateHelpEvent.TYPE, shim);
      
      events.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         public void onSessionInit(SessionInitEvent sie)
         {
            if (session.getSessionInfo().getShowHelpHome())
            {
               shim.bringToFront();
            }
         }
      });
   }
}
