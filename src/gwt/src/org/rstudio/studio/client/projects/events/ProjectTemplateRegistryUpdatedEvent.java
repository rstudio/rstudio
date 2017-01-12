/*
 * ProjectTemplateRegistryUpdatedEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.projects.events;

import org.rstudio.studio.client.projects.model.ProjectTemplateRegistry;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ProjectTemplateRegistryUpdatedEvent
      extends GwtEvent<ProjectTemplateRegistryUpdatedEvent.Handler>
{
   public ProjectTemplateRegistryUpdatedEvent(ProjectTemplateRegistry data)
   {
      data_ = data;
   }
   
   public ProjectTemplateRegistry getData()
   {
      return data_;
   }
   
   private final ProjectTemplateRegistry data_;
   
   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onProjectTemplateRegistryUpdated(ProjectTemplateRegistryUpdatedEvent event);
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onProjectTemplateRegistryUpdated(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();

}
