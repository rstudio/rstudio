/*
 * ShowDataEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;

public class ShowDataEvent extends GwtEvent<ShowDataHandler>
{
   public static final GwtEvent.Type<ShowDataHandler> TYPE =
      new GwtEvent.Type<ShowDataHandler>();
   
   public ShowDataEvent(DataItem data)
   {
      data_ = data;
   }
   
   public DataItem getData()
   {
      return data_;
   }
   
   @Override
   protected void dispatch(ShowDataHandler handler)
   {
      handler.onShowData(this);
   }

   @Override
   public GwtEvent.Type<ShowDataHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private DataItem data_;
}

