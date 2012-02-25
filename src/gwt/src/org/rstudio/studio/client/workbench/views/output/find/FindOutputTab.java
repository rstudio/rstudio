/*
 * FindOutputTab.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.core.client.GWT;
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
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesState;

public class FindOutputTab extends DelayLoadWorkbenchTab<FindOutputPresenter>
{
   public abstract static class Shim extends DelayLoadTabShim<FindOutputPresenter, FindOutputTab>
   {
      @Handler
      abstract void onFindInFiles();
      abstract void initialize(FindInFilesState state);
      public abstract void onDismiss();
   }

   static interface Binder extends CommandBinder<Commands, Shim>
   {}

   @Inject
   public FindOutputTab(final Shim shim,
                        EventBus events,
                        Commands commands,
                        final Session session)
   {
      super("Find", shim);
      shim_ = shim;

      if (!session.getSessionInfo().isFindInFilesEnabled())
      {
         commands.findInFiles().setVisible(false);
         return;
      }

      events.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         @Override
         public void onSessionInit(SessionInitEvent sie)
         {
            FindInFilesState state =
                                 session.getSessionInfo().getFindInFilesState();
            if (state.isTabVisible())
               shim.initialize(state);
         }
      });

      GWT.<Binder>create(Binder.class).bind(commands, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }

   public void onDismiss()
   {
      shim_.onDismiss();
   }

   private final Shim shim_;
}
