/*
 * ViewDataEvent.java
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
package org.rstudio.studio.client.workbench.views.data.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.data.model.DataView;

public class ViewDataEvent extends GwtEvent<ViewDataHandler>
{
   public static final GwtEvent.Type<ViewDataHandler> TYPE =
      new GwtEvent.Type<ViewDataHandler>();
   
   public ViewDataEvent(DataView dataView)
   {
      dataView_ = dataView;
   }
   
   public DataView getDataView()
   {
      return dataView_;
   }
   
   @Override
   protected void dispatch(ViewDataHandler handler)
   {
      handler.onViewData(this);
   }

   @Override
   public GwtEvent.Type<ViewDataHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private DataView dataView_;
}

