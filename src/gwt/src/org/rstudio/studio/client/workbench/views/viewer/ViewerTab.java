/*
 * ViewerTab.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.viewer;

import com.google.inject.Inject;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.plumber.events.PlumberAPIStatusEvent;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerClearedEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerNavigateEvent;
import org.rstudio.studio.client.workbench.views.viewer.events.ViewerPreviewRmdEvent;

public class ViewerTab extends DelayLoadWorkbenchTab<ViewerPresenter>
{
   public abstract static class Shim
        extends DelayLoadTabShim<ViewerPresenter, ViewerTab>
        implements ViewerNavigateEvent.Handler,
                   ViewerPreviewRmdEvent.Handler,
                   ViewerClearedEvent.Handler,
                   ShinyApplicationStatusEvent.Handler,
                   PlumberAPIStatusEvent.Handler {}

   @Inject
   public ViewerTab(Shim shim, Session session, EventBus eventBus)
   {
      super("Viewer", shim);
      session_ = session;

      eventBus.addHandler(ViewerNavigateEvent.TYPE, shim);
      eventBus.addHandler(ViewerPreviewRmdEvent.TYPE, shim);
      eventBus.addHandler(ViewerClearedEvent.TYPE, shim);
      eventBus.addHandler(ShinyApplicationStatusEvent.TYPE, shim);
      eventBus.addHandler(PlumberAPIStatusEvent.TYPE, shim);
   }

   @SuppressWarnings("unused")
   private Session session_;
}
