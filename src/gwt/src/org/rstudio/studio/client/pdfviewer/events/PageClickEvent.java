/*
 * PageClickEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.pdfviewer.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.pdfviewer.model.SyncTexCoordinates;

public class PageClickEvent extends GwtEvent<PageClickEvent.Handler>
{
   public interface HasPageClickHandlers
   {
      HandlerRegistration addPageClickHandler(PageClickEvent.Handler handler);
   }

   public interface Handler extends EventHandler
   {
      void onPageClick(PageClickEvent event);
   }

   public PageClickEvent(SyncTexCoordinates coordinates)
   {
      coordinates_ = coordinates;
   }

   public SyncTexCoordinates getCoordinates()
   {
      return coordinates_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPageClick(this);
   }

   private final SyncTexCoordinates coordinates_;

   public static Type<Handler> TYPE = new Type<Handler>();
}
