/*
 * JobsTab.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.jobs;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.jobs.events.JobUpdatedEvent;

public class JobsTab extends DelayLoadWorkbenchTab<JobsPresenter>
{
   public abstract static class Shim 
        extends DelayLoadTabShim<JobsPresenter, JobsTab>
        implements JobUpdatedEvent.Handler 
   {
      
   }
   
   public interface Binder extends CommandBinder<Commands, JobsTab.Shim> {}

   @Inject
   public JobsTab(final Shim shim, 
                        Binder binder,
                        Commands commands,
                        EventBus eventBus,
                        Session session)
   {
      super("Jobs", shim);
      binder.bind(commands, shim);
      session_ = session;
      eventBus_ = eventBus;
      eventBus.addHandler(JobUpdatedEvent.TYPE, shim);
   }
   
   private final Session session_;
   private final EventBus eventBus_;
}