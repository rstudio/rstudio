/*
 * Presentation2Tab.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.workbench.views.presentation2;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.presentation2.events.PresentationPreviewEvent;

import com.google.inject.Inject;

public class Presentation2Tab extends DelayLoadWorkbenchTab<Presentation2>
{
   public interface Binder extends CommandBinder<Commands, Presentation2Tab.Shim> {}
   
   public abstract static class Shim 
   extends DelayLoadTabShim<Presentation2, Presentation2Tab> 
   implements PresentationPreviewEvent.Handler {
      @Handler
      public abstract void onRefreshPresentation2();
      @Handler
      public abstract void onPresentation2Home();
      @Handler
      public abstract void onPresentation2Next();
      @Handler
      public abstract void onPresentation2Prev();
      @Handler
      public abstract void onPresentation2Edit();
      @Handler
      public abstract void onPresentation2Print();
      @Handler
      public abstract void onPresentation2Present();
      @Handler
      public abstract void onPresentation2PresentFromBeginning();
   }
   
   @Inject
   public Presentation2Tab(Shim shim, Binder binder, Session session, Commands commands, EventBus eventBus)
   {
      // This should always be title "Presentation" (rather than the name of the underlying
      // tab "Presentations". The proper name is "Presentation", we just used
      // "Presentations" so the configurations wouldn't conflict.
      super(constants_.presentationTitle(), shim);
      session_ = session;
      binder.bind(commands, shim);
      eventBus.addHandler(PresentationPreviewEvent.TYPE, shim);
      
      eventBus.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         // if the other presentation tab is active then remove our commands
         if (isSuppressed())
         {
            commands.layoutZoomPresentation2().remove();
            commands.activatePresentation2().remove();
            commands.refreshPresentation2().remove();
            commands.presentation2Home().remove();
            commands.presentation2Next().remove();
            commands.presentation2Prev().remove();
            commands.presentation2Edit().remove();
            commands.presentation2Print().remove();
            commands.presentation2Present().remove();
            commands.presentation2PresentFromBeginning().remove();
         }
         
      });
   }
   
   // requires quarto and the legacy presentation tab be not active
   @Override
   public boolean isSuppressed()
   {
      SessionInfo si = session_.getSessionInfo();
      return !si.getQuartoConfig().enabled ||
             si.getPresentationState().isActive();
   }
   
   private final Session session_;
   private static final Presentation2Constants constants_ = com.google.gwt.core.client.GWT.create(Presentation2Constants.class);
}
