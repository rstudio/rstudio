/*
 * JobProgressEvent.java
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

public class JobProgressEvent extends GwtEvent<JobProgressEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobProgress(JobProgressEvent event);
   }
   
   // constructor used when there's no progress
   public JobProgressEvent()
   {
      name_ = null;
      units_ = 0;
      max_ = 0;
      elapsed_ = 0;
   }
   
   // constructor used when jobs are running
   public JobProgressEvent(String name, int units, int max, int elapsed)
   {
      name_ = name;
      units_ = units;
      max_ = max;
      elapsed_ = elapsed;
   }

   public int units()
   {
      return units_;
   }
   
   public int max()
   {
      return max_;
   }
   
   public int elapsed()
   {
      return elapsed_;
   }
   
   public String name()
   {
      return name_;
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

   private final String name_;
   private final int units_;
   private final int max_;
   private final int elapsed_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}