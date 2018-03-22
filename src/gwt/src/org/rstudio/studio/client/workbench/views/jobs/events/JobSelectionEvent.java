/*
 * JobSelectionEvent.java
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
package org.rstudio.studio.client.workbench.views.jobs.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class JobSelectionEvent extends GwtEvent<JobSelectionEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobSelection(JobSelectionEvent event);
   }
   
   public JobSelectionEvent(String id, boolean selected)
   {
      id_ = id;
      selected_ = selected;
   }

   public String id()
   {
      return id_;
   }
   
   public boolean selected()
   {
      return selected_;
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
   private final boolean selected_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}