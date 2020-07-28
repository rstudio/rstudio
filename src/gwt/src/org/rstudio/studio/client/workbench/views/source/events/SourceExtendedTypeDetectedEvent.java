/*
 * SourceExtendedTypeDetectedEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SourceExtendedTypeDetectedEvent extends GwtEvent<SourceExtendedTypeDetectedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final String getDocId() /*-{
         return this.doc_id;
      }-*/;

      public native final String getExtendedType() /*-{
         return this.extended_type;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onSourceExtendedTypeDetected(SourceExtendedTypeDetectedEvent e);
   }

   public SourceExtendedTypeDetectedEvent(Data data)
   {
      data_ = data;
   }

   public String getDocId()
   {
      return data_.getDocId();
   }

   public String getExtendedType()
   {
      return data_.getExtendedType();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSourceExtendedTypeDetected(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
