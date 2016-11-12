/*
 * TerminalTab.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.inject.Inject;

import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateTerminalEvent;

public class TerminalTab extends DelayLoadWorkbenchTab<TerminalTabPresenter> 
                         implements ProvidesBusy
{
   public abstract static class Shim extends
                DelayLoadTabShim<TerminalTabPresenter, TerminalTab>
      implements ProvidesBusy,
                 CreateTerminalEvent.Handler
   {
      abstract void initialize();
   }

   @Inject
   public TerminalTab(Shim shim, EventBus events, final Session session)
   {
      super("Terminal", shim);
      shim_ = shim;

      events.addHandler(CreateTerminalEvent.TYPE, shim);
   }

   @Override
   public void addBusyHandler(BusyHandler handler)
   {
      shim_.addBusyHandler(handler);
   }
   
   private Shim shim_;
}