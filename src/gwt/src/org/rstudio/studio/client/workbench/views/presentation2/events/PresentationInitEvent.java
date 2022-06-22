/*
 * PresentationInitEvent.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.presentation2.events;

import org.rstudio.studio.client.workbench.views.presentation2.model.RevealSlide;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PresentationInitEvent extends GwtEvent<PresentationInitEvent.Handler>
{ 
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public final native JsArray<RevealSlide> getSlides() /*-{
         return this.slides;
      }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onPresentationInit(PresentationInitEvent event);
   }

   public PresentationInitEvent(Data data)
   {
      slides_ = data.getSlides();
   }

   public JsArray<RevealSlide> getSlides()
   {
      return slides_;
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
   
   private final JsArray<RevealSlide> slides_;
}
