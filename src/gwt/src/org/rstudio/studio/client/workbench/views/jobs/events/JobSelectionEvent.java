/*
 * JobSelectionEvent.java
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

public class JobSelectionEvent extends GwtEvent<JobSelectionEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobSelection(JobSelectionEvent event);
   }

   public JobSelectionEvent(String id, int jobType, boolean selected, boolean animate)
   {
      id_ = id;
      jobType_ = jobType;
      selected_ = selected;
      animate_ = animate;
   }

   public String id()
   {
      return id_;
   }

   public int jobType()
   {
      return jobType_;
   }

   public boolean selected()
   {
      return selected_;
   }

   public boolean animate()
   {
      return animate_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJobSelection(this);
   }

   private final String id_;
   private final int jobType_;
   private final boolean selected_;
   private final boolean animate_;

   public static final Type<Handler> TYPE = new Type<>();
}
