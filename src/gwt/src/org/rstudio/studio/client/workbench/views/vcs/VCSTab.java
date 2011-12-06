/*
 * VCSTab.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.inject.Inject;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;

public class VCSTab extends DelayLoadWorkbenchTab<VCSPresenter>
                            
{
   public abstract static class VCSShim extends DelayLoadTabShim<VCSPresenter, VCSTab>
                                        implements ShowVcsHistoryEvent.Handler
   {
   }

   @Inject
   protected VCSTab(VCSShim shim, EventBus eventBus, Session session)
   {
      super(session.getSessionInfo().getVcsName(), shim);
      session_ = session;
      eventBus.addHandler(ShowVcsHistoryEvent.TYPE, shim);
   }

   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().isVcsEnabled();
   }

   private final Session session_;
}
