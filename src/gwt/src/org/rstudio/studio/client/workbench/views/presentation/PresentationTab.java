/*
 * PresentationTab.java
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
package org.rstudio.studio.client.workbench.views.presentation;


import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.presentation.events.ShowPresentationPaneEvent;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;


public class PresentationTab extends DelayLoadWorkbenchTab<Presentation>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}

   public abstract static class Shim extends DelayLoadTabShim<Presentation, PresentationTab>
                                     implements ShowPresentationPaneEvent.Handler
   {
      abstract void initialize(PresentationState state);
      abstract void confirmClose(Command onConfirmed);
   }

   @Inject
   public PresentationTab(final Shim shim,
                          Binder binder,
                          final Commands commands,
                          EventBus eventBus,
                          Session session)
   {
      super(session.getSessionInfo().getPresentationName(), shim);
      binder.bind(commands, shim);
      shim_ = shim;
      session_ = session;

      eventBus.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         PresentationState state = session_.getSessionInfo().getPresentationState();
         if (state.isActive())
            shim.initialize(state);
      });

      eventBus.addHandler(ShowPresentationPaneEvent.TYPE, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }

   @Override
   public void confirmClose(Command onConfirmed)
   {
      shim_.confirmClose(onConfirmed);
   }

   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().getPresentationState().isActive();
   }

   private Session session_;
   private Shim shim_;
}
