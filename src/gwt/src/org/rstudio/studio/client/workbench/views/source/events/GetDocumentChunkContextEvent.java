/*
 * GetDocumentChunkContextEvent.java
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

import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

public class GetDocumentChunkContextEvent
      extends CrossWindowEvent<GetDocumentChunkContextEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
   }
   
   public GetDocumentChunkContextEvent()
   {
      data_ = JavaScriptObject.createObject().cast();
   }
   
   public GetDocumentChunkContextEvent(Data data)
   {
      data_ = data;
   }
   
   public Data getData()
   {
      return data_;
   }
   
   private final Data data_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onGetDocumentChunkContext(GetDocumentChunkContextEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onGetDocumentChunkContext(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
}

