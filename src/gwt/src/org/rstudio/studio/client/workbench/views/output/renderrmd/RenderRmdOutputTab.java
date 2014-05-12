
/*
 * RenderRmdOutputTab.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.output.renderrmd;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderCompletedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderStartedEvent;
import org.rstudio.studio.client.workbench.events.BusyHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class RenderRmdOutputTab 
   extends DelayLoadWorkbenchTab<RenderRmdOutputPresenter> 
   implements ProvidesBusy
{
   public abstract static class Shim extends
                DelayLoadTabShim<RenderRmdOutputPresenter, RenderRmdOutputTab>
      implements RmdRenderStartedEvent.Handler,
                 RmdRenderOutputEvent.Handler,
                 RmdRenderCompletedEvent.Handler,
                 RestartStatusEvent.Handler,
                 ProvidesBusy
   {
      abstract void initialize();
      abstract void confirmClose(Command onConfirmed);
   }

   @Inject
   public RenderRmdOutputTab(Shim shim,
                             EventBus events,
                             final Session session)
   {
      super("R Markdown", shim);
      shim_ = shim;

      events.addHandler(RmdRenderStartedEvent.TYPE, shim);
      events.addHandler(RmdRenderOutputEvent.TYPE, shim);
      events.addHandler(RmdRenderCompletedEvent.TYPE, shim);
      events.addHandler(RestartStatusEvent.TYPE, shim);
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