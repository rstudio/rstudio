/*
 * Presentation2Tab.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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


package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

import com.google.inject.Inject;

public class Presentation2Tab extends DelayLoadWorkbenchTab<Presentation2>
{
   public abstract static class Shim 
   extends DelayLoadTabShim<Presentation2, Presentation2Tab> {}
   
   @Inject
   public Presentation2Tab(Shim shim, Session session, Commands commands, EventBus eventBus)
   {
      super("Presentation", shim);
      session_ = session;
      
      eventBus.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         // if the other presentation tab is active then remove our commands
         if (isSuppressed())
         {
            commands.layoutZoomPresentation2().remove();
            commands.activatePresentation2().remove();
         }
         
      });
   }
   
   // requires quarto and the legacy presentation tab be not active
   @Override
   public boolean isSuppressed()
   {
      SessionInfo si = session_.getSessionInfo();
      return !si.getQuartoConfig().installed ||
             si.getPresentationState().isActive();
   }
   
   private final Session session_;
}
