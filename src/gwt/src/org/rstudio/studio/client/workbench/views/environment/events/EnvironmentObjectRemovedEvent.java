/*
 * EnvironmentObjectRemovedEvent.java
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

public class EnvironmentObjectRemovedEvent 
                              extends GwtEvent<EnvironmentObjectRemovedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onEnvironmentObjectRemoved(EnvironmentObjectRemovedEvent event);
   }

   public static final GwtEvent.Type<EnvironmentObjectRemovedEvent.Handler> TYPE =
      new GwtEvent.Type<EnvironmentObjectRemovedEvent.Handler>();
   
   public EnvironmentObjectRemovedEvent(String objectName)
   {
      objectName_ = objectName;
   }
   
   public String getObjectName()
   {
      return objectName_;
   }
   
   @Override
   protected void dispatch(EnvironmentObjectRemovedEvent.Handler handler)
   {
      handler.onEnvironmentObjectRemoved(this);
   }

   @Override
   public GwtEvent.Type<EnvironmentObjectRemovedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String objectName_;
}
