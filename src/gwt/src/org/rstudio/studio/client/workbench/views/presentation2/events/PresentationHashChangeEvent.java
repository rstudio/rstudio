/*
 * PresentationHashChangeEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.presentation2.events;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PresentationHashChangeEvent extends GwtEvent<PresentationHashChangeEvent.Handler>
{ 
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public final native String getHRef() /*-{
         return this.href;
      }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onPresentationInit(PresentationHashChangeEvent event);
   }

   public PresentationHashChangeEvent(Data data)
   {
      data_ = data;
   }

   public String getHRef()
   {
      return data_.getHRef();
   }
  
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPresentationInit(this);
   }


   public static final Type<Handler> TYPE = new Type<>();
   
   private final Data data_;
}
