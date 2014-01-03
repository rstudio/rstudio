/*
 * ShowShinyApplicationEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.events;

import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShowShinyApplicationEvent extends GwtEvent<ShowShinyApplicationEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onShowShinyApplication(ShowShinyApplicationEvent event);
   }

   public static final GwtEvent.Type<ShowShinyApplicationEvent.Handler> TYPE =
      new GwtEvent.Type<ShowShinyApplicationEvent.Handler>();
   
   public ShowShinyApplicationEvent(ShinyApplicationParams params)
   {
      params_ = params;
   }
   
   public ShinyApplicationParams getParams()
   {
      return params_;
   }
   
   @Override
   protected void dispatch(ShowShinyApplicationEvent.Handler handler)
   {
      handler.onShowShinyApplication(this);
   }

   @Override
   public GwtEvent.Type<ShowShinyApplicationEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private ShinyApplicationParams params_;
}
