/*
 * ViewerPreviewRmdEvent.java
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
package org.rstudio.studio.client.workbench.views.viewer.events;

import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ViewerPreviewRmdEvent extends GwtEvent<ViewerPreviewRmdEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onViewerPreviewRmd(ViewerPreviewRmdEvent event);
   }

   public ViewerPreviewRmdEvent(RmdPreviewParams params, boolean maximize)
   {
      params_ = params;
      maximize_ = maximize;
   }

   public RmdPreviewParams getParams()
   {
      return params_;
   }

   public boolean getMaximize()
   {
      return params_.getResult().getForceMaximize() || maximize_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onViewerPreviewRmd(this);
   }

   private final RmdPreviewParams params_;
   private final boolean maximize_;

   public static final Type<Handler> TYPE = new Type<>();
}
