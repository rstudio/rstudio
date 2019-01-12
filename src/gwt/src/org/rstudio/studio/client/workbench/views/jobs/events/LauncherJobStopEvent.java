/*
 * LauncherJobStopEvent.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

public class LauncherJobStopEvent extends GwtEvent<LauncherJobStopEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onLauncherJobStop(LauncherJobStopEvent event);
   }
   
   public LauncherJobStopEvent(String jobId, int state)
   {
      jobId_ = jobId;
      state_ = state;
   }

   public String getJobId()
   {
      return jobId_;
   }
   
   public int getState()
   {
      return state_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onLauncherJobStop(this);
   }

   private final String jobId_;
   private final int state_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
