/*
 * WorkspaceObjectRemovedEvent.java
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
package org.rstudio.studio.client.workbench.views.workspace.events;

import com.google.gwt.event.shared.GwtEvent;

public class WorkspaceObjectRemovedEvent 
                              extends GwtEvent<WorkspaceObjectRemovedHandler>
{
   public static final GwtEvent.Type<WorkspaceObjectRemovedHandler> TYPE =
      new GwtEvent.Type<WorkspaceObjectRemovedHandler>();
   
   public WorkspaceObjectRemovedEvent(String objectName)
   {
      objectName_ = objectName;
   }
   
   public String getObjectName()
   {
      return objectName_;
   }
   
   @Override
   protected void dispatch(WorkspaceObjectRemovedHandler handler)
   {
      handler.onWorkspaceObjectRemoved(this);
   }

   @Override
   public GwtEvent.Type<WorkspaceObjectRemovedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String objectName_;
}
