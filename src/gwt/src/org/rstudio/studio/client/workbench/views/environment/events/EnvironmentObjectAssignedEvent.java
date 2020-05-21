/*
 * EnvironmentObjectAssignedEvent.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.studio.client.workbench.views.environment.model.RObject;

public class EnvironmentObjectAssignedEvent 
                        extends GwtEvent<EnvironmentObjectAssignedEvent.Handler>
{

   public interface Handler extends EventHandler
   {
      void onEnvironmentObjectAssigned(EnvironmentObjectAssignedEvent event);
   }

   public static final GwtEvent.Type<EnvironmentObjectAssignedEvent.Handler> TYPE =
      new GwtEvent.Type<EnvironmentObjectAssignedEvent.Handler>();
   
   public EnvironmentObjectAssignedEvent(RObject objectInfo)
   {
      objectInfo_ = objectInfo;
   }
   
   public RObject getObjectInfo()
   {
      return objectInfo_;
   }
   
   @Override
   protected void dispatch(EnvironmentObjectAssignedEvent.Handler handler)
   {
      handler.onEnvironmentObjectAssigned(this);
   }

   @Override
   public GwtEvent.Type<EnvironmentObjectAssignedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final RObject objectInfo_;
}
