/*
 * VCSTab.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsDiffEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.ShowVcsHistoryEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRevertFileEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsViewOnGitHubEvent;

public class VCSTab extends DelayLoadWorkbenchTab<VCSPresenter>
                            
{
   public interface Binder extends CommandBinder<Commands, VCSTab.VCSShim> {}
   
   public abstract static class VCSShim extends DelayLoadTabShim<VCSPresenter, VCSTab>
                                        implements ShowVcsHistoryEvent.Handler,
                                                   ShowVcsDiffEvent.Handler,
                                                   VcsRevertFileEvent.Handler,
                                                   VcsViewOnGitHubEvent.Handler
   {
      @Handler
      public abstract void onVcsCommit();
      @Handler
      public abstract void onVcsShowHistory();
      @Handler
      public abstract void onVcsPull();
      @Handler
      public abstract void onVcsPullRebase();
      @Handler
      public abstract void onVcsPush();
      @Handler
      public abstract void onVcsCleanup();
      @Handler
      public abstract void onVcsIgnore();
   }

   @Inject
   protected VCSTab(VCSShim shim, 
                    Commands commands,
                    Binder binder,
                    EventBus eventBus, 
                    Session session)
   {
      super(session.getSessionInfo().getVcsName(), shim);
      binder.bind(commands, shim);
      session_ = session;
      eventBus.addHandler(ShowVcsHistoryEvent.TYPE, shim);
      eventBus.addHandler(ShowVcsDiffEvent.TYPE, shim);
      eventBus.addHandler(VcsRevertFileEvent.TYPE, shim);
      eventBus.addHandler(VcsViewOnGitHubEvent.TYPE, shim);
   }

   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().isVcsEnabled();
   }

   private final Session session_;
}
