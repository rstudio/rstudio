/*
 * BuildStatusEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.buildtools.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildStatusEvent extends GwtEvent<BuildStatusEvent.Handler>
{  
   public final static String STATUS_STARTED = "started";
   public final static String STATUS_COMPLETED = "completed";
   
   public interface Handler extends EventHandler
   {
      void onBuildStatus(BuildStatusEvent event);
   }

   public BuildStatusEvent(String status)
   {
      status_ = status;
   }
    
   public String getStatus()
   {
      return status_;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onBuildStatus(this);
   }
   
   private final String status_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
