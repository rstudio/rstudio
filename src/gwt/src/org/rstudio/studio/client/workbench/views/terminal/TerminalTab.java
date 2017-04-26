/*
 * TerminalTab.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateTerminalEvent;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class TerminalTab extends DelayLoadWorkbenchTab<TerminalTabPresenter>
                         implements ProvidesBusy
{
   public interface Binder extends CommandBinder<Commands, Shim> {}

   public abstract static class Shim 
      extends DelayLoadTabShim<TerminalTabPresenter, TerminalTab>
      implements ProvidesBusy,
                 CreateTerminalEvent.Handler,
                 SessionInitHandler
   {
      @Handler
      public abstract void onActivateTerminal();

      @Handler
      public abstract void onCloseTerminal();

      @Handler
      public abstract void onRenameTerminal();

      @Handler
      public abstract void onClearTerminalScrollbackBuffer();

      @Handler
      public abstract void onPreviousTerminal();

      @Handler
      public abstract void onNextTerminal();

      @Handler
      public abstract void onShowTerminalInfo();

      abstract void initialize();
      abstract void confirmClose(Command onConfirmed);
   }

   @Inject
   public TerminalTab(Shim shim,
                      EventBus events,
                      Commands commands,
                      Binder binder,
                      final Session session)
   {
      super("Terminal", shim);
      shim_ = shim;

      binder.bind(commands, shim_);
      events.addHandler(CreateTerminalEvent.TYPE, shim_);
      events.addHandler(SessionInitEvent.TYPE, shim_);

      shim_.initialize();
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
   public void addBusyHandler(BusyHandler handler)
   {
      shim_.addBusyHandler(handler);
   }

   private Shim shim_;
}