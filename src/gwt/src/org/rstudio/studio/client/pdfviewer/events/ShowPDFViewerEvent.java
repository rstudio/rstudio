/*
 * ShowPDFViewerEvent.java
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
package org.rstudio.studio.client.pdfviewer.events;

import com.google.gwt.event.shared.GwtEvent;

public class ShowPDFViewerEvent extends GwtEvent<ShowPDFViewerHandler>
{ 
   public static final GwtEvent.Type<ShowPDFViewerHandler> TYPE =
      new GwtEvent.Type<ShowPDFViewerHandler>();
   
   public ShowPDFViewerEvent()
   {
   }
   
   @Override
   protected void dispatch(ShowPDFViewerHandler handler)
   {
      handler.onShowPDFViewer(this);
   }

   @Override
   public GwtEvent.Type<ShowPDFViewerHandler> getAssociatedType()
   {
      return TYPE;
   }
}
