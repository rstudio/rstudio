/*
 * JobExecuteActionEvent.java
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
package org.rstudio.studio.client.workbench.views.jobs.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class JobExecuteActionEvent extends GwtEvent<JobExecuteActionEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobExecuteAction(JobExecuteActionEvent event);
   }

   public JobExecuteActionEvent(String id, String action)
   {
      id_ = id;
      action_ = action;
   }

   public String id()
   {
      return id_;
   }

   public String action()
   {
      return action_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJobExecuteAction(this);
   }

   private final String id_;
   private final String action_;

   public static final Type<Handler> TYPE = new Type<>();
}
