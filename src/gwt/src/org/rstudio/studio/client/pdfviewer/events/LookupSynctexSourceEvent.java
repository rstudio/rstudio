/*
 * PageClickEvent.java
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
package org.rstudio.studio.client.pdfviewer.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;

public class LookupSynctexSourceEvent extends GwtEvent<LookupSynctexSourceEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onLookupSynctexSource(LookupSynctexSourceEvent event);
   }

   public LookupSynctexSourceEvent(SyncTexCoordinates coordinates,
                         boolean fromClick)
   {
      coordinates_ = coordinates;
      fromClick_ = fromClick;
   }

   public SyncTexCoordinates getCoordinates()
   {
      return coordinates_;
   }

   public boolean fromClick()
   {
      return fromClick_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onLookupSynctexSource(this);
   }

   private final SyncTexCoordinates coordinates_;
   private final boolean fromClick_;

   public static Type<Handler> TYPE = new Type<>();
}
