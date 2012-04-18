/*
 * WorkspaceObjectAssignedEvent.java
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
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceObjectInfo;

public class WorkspaceObjectAssignedEvent 
                        extends GwtEvent<WorkspaceObjectAssignedHandler>
{
   public static final GwtEvent.Type<WorkspaceObjectAssignedHandler> TYPE =
      new GwtEvent.Type<WorkspaceObjectAssignedHandler>();
   
   public WorkspaceObjectAssignedEvent(WorkspaceObjectInfo objectInfo)
   {
      objectInfo_ = objectInfo ;
   }
   
   public WorkspaceObjectInfo getObjectInfo()
   {
      return objectInfo_;
   }
   
   @Override
   protected void dispatch(WorkspaceObjectAssignedHandler handler)
   {
      handler.onWorkspaceObjectAssigned(this);
   }

   @Override
   public GwtEvent.Type<WorkspaceObjectAssignedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private WorkspaceObjectInfo objectInfo_;
}
