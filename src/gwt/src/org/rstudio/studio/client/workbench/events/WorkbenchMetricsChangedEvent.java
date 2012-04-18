/*
 * WorkbenchMetricsChangedEvent.java
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
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;

public class WorkbenchMetricsChangedEvent extends 
                                       GwtEvent<WorkbenchMetricsChangedHandler>
{
   public static final GwtEvent.Type<WorkbenchMetricsChangedHandler> TYPE =
      new GwtEvent.Type<WorkbenchMetricsChangedHandler>();
   
   public WorkbenchMetricsChangedEvent(WorkbenchMetrics clientMetrics)
   {
      clientMetrics_ = clientMetrics ;
   }
   
   public WorkbenchMetrics getWorkbenchMetrics()
   {
      return clientMetrics_ ;
   }
   
   @Override
   protected void dispatch(WorkbenchMetricsChangedHandler handler)
   {
      handler.onWorkbenchMetricsChanged(this);
   }

   @Override
   public GwtEvent.Type<WorkbenchMetricsChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final WorkbenchMetrics clientMetrics_ ;
}
