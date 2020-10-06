/*
 * CompilePdfOutputTab.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfErrorsEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfOutputEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfStartedEvent;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfState;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.output.compilepdf.events.CompilePdfEvent;

public class CompilePdfOutputTab
   extends DelayLoadWorkbenchTab<CompilePdfOutputPresenter>
   implements ProvidesBusy
{
   public abstract static class Shim extends
                DelayLoadTabShim<CompilePdfOutputPresenter, CompilePdfOutputTab>
      implements CompilePdfEvent.Handler,
                 CompilePdfStartedEvent.Handler,
                 CompilePdfOutputEvent.Handler,
                 CompilePdfErrorsEvent.Handler,
                 CompilePdfCompletedEvent.Handler,
                 ProvidesBusy
   {
      abstract void initialize(CompilePdfState compilePdfState);
      abstract void confirmClose(Command onConfirmed);
      @Handler abstract void onActivateCompilePDF();
   }

   interface Binder extends CommandBinder<Commands, Shim> {}

   @Inject
   public CompilePdfOutputTab(Shim shim,
                              EventBus events,
                              Commands commands,
                              final Session session)
   {
      super("Compile PDF", shim);
      shim_ = shim;

      events.addHandler(CompilePdfEvent.TYPE, shim);
      events.addHandler(CompilePdfOutputEvent.TYPE, shim);
      events.addHandler(CompilePdfErrorsEvent.TYPE, shim);
      events.addHandler(CompilePdfStartedEvent.TYPE, shim);
      events.addHandler(CompilePdfCompletedEvent.TYPE, shim);
      GWT.<Binder>create(Binder.class).bind(commands, shim);

      events.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         SessionInfo sessionInfo = session.getSessionInfo();
         CompilePdfState compilePdfState = sessionInfo.getCompilePdfState();
         if (compilePdfState.isTabVisible())
            shim_.initialize(compilePdfState);
      });
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
   public void addBusyHandler(BusyEvent.Handler handler)
   {
      shim_.addBusyHandler(handler);
   }

   private final Shim shim_;

}
