/*
 * DocumentCloseEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.server.model;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Request closure of an open file, by docId; will prompt if file is dirty
 */
public class DocumentCloseEvent extends GwtEvent<DocumentCloseEvent.Handler>
{
   public DocumentCloseEvent(String docId)
   {
      docId_ = docId;
   }

   public interface Handler extends EventHandler
   {
      void onDocumentClose(DocumentCloseEvent event);
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class Data
   {
      public String sample;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDocumentClose(this);
   }

   public String getDocId()
   {
      return docId_;
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final String docId_;
}
