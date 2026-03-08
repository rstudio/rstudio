/*
 * SaveDocumentEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class SaveDocumentEvent extends CrossWindowEvent<SaveDocumentEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public final native String getId() /*-{ return this["id"]; }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onSaveDocument(SaveDocumentEvent event);
   }

   public SaveDocumentEvent()
   {
      this(null);
   }

   public SaveDocumentEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   private final Data data_;

   // Boilerplate ----

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSaveDocument(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

}
