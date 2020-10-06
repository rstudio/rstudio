
/*
 * DataOutputTab.java
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

package org.rstudio.studio.client.workbench.views.output.data;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.output.data.events.DataOutputCompletedEvent;

public class DataOutputTab
   extends DelayLoadWorkbenchTab<DataOutputPresenter>
{
   public abstract static class Shim extends
                DelayLoadTabShim<DataOutputPresenter, DataOutputTab>
      implements DataOutputCompletedEvent.Handler
   {
      abstract void initialize();
      abstract void confirmClose(Command onConfirmed);
      @Handler abstract void onActivateSQLResults();
   }

   interface Binder extends CommandBinder<Commands, Shim> {}

   @Inject
   public DataOutputTab(Shim shim,
                        EventBus events,
                        Commands commands,
                        final Session session)
   {
      super("SQL Results", shim);
      shim_ = shim;

      GWT.<Binder>create(Binder.class).bind(commands, shim);
      events.addHandler(DataOutputCompletedEvent.TYPE, shim);
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

   private final Shim shim_;
}
