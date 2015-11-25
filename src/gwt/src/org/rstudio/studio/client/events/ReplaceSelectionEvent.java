/*
 * ReplaceSelectionEvent.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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
package org.rstudio.studio.client.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

@JavaScriptSerializable
public class ReplaceSelectionEvent
   extends CrossWindowEvent<ReplaceSelectionEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native String getText() /*-{ return this["text"]; }-*/;
      public final native String getId() /*-{ return this["id"]; }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onReplaceSelection(ReplaceSelectionEvent event);
   }
   
   public ReplaceSelectionEvent()
   {
      this(null);
   }
   
   public ReplaceSelectionEvent(Data data)
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
      handler.onReplaceSelection(this);
   }
   
   public static final Type<Handler> TYPE = new Type<Handler>();

}
