/*
 * JobProgressEvent.java
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

import org.rstudio.studio.client.workbench.views.jobs.model.LocalJobProgress;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class JobProgressEvent extends GwtEvent<JobProgressEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobProgress(JobProgressEvent event);
   }

   // constructor used when there's no progress
   public JobProgressEvent()
   {
      progress_ = null;
   }

   public JobProgressEvent(LocalJobProgress progress)
   {
      progress_ = progress;
   }

   public boolean hasProgress()
   {
      return progress_ != null;
   }

   public LocalJobProgress progress()
   {
      return progress_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJobProgress(this);
   }

   private final LocalJobProgress progress_;

   public static final Type<Handler> TYPE = new Type<>();
}
