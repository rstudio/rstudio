/*
 * PlotsChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.plots.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsState;

public class PlotsChangedEvent extends GwtEvent<PlotsChangedHandler>
{
   public static final GwtEvent.Type<PlotsChangedHandler> TYPE =
      new GwtEvent.Type<PlotsChangedHandler>();
     
   public PlotsChangedEvent(PlotsState plotsState)
   {
      plotsState_ = plotsState;
   }
   
   public PlotsState getPlotsState()
   {
      return plotsState_;
   }
   
   @Override
   protected void dispatch(PlotsChangedHandler handler)
   {
      handler.onPlotsChanged(this);
   }

   @Override
   public GwtEvent.Type<PlotsChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private PlotsState plotsState_;
}
