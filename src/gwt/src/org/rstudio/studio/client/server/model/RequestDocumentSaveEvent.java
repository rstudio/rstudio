/*
 * RequestDocumentSaveEvent.java
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
package org.rstudio.studio.client.server.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RequestDocumentSaveEvent extends GwtEvent<RequestDocumentSaveEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final JsArrayString getDocumentIds()
      /*-{
         return this["ids"];
      }-*/;

   }

   public RequestDocumentSaveEvent(Data data)
   {
      data_ = data;
   }

   public JsArrayString getDocumentIds()
   {
      return data_.getDocumentIds();
   }

   private final Data data_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onRequestDocumentSave(RequestDocumentSaveEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRequestDocumentSave(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
