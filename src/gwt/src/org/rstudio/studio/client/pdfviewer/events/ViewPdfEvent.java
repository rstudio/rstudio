/*
 * ViewPdfEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.GwtEvent;

public class ViewPdfEvent extends GwtEvent<ViewPdfHandler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public final native String getPdfUrl() /*-{
         return this.pdf_url;
      }-*/;
   }
   
   public static final GwtEvent.Type<ViewPdfHandler> TYPE =
      new GwtEvent.Type<ViewPdfHandler>();
   
   public ViewPdfEvent(Data data)
   {
      data_ = data;
   }
   
   public String getPdfUrl()
   {
      return data_.getPdfUrl();
   }
   
   @Override
   protected void dispatch(ViewPdfHandler handler)
   {
      handler.onViewPdf(this);
   }

   @Override
   public GwtEvent.Type<ViewPdfHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final Data data_;
}
