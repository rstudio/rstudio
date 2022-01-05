/*
 * PresentationSlideChangeEvent.java
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
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PresentationSlideChangeEvent extends GwtEvent<PresentationSlideChangeEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public static final native Data withSlideIndex(Data data, int index) /*-{
         return {
            slide: data.slide,
            index: index,
            first: data.first,
            last: data.last
         }
      }-*/;

      public final native RevealSlide getSlide() /*-{
         return this.slide;
      }-*/;
      
      public final native int getSlideIndex() /*-{
         return this.index;
      }-*/;
      
      public final native boolean isFirst() /*-{
         return this.first;
      }-*/;
      
      public final native boolean isLast() /*-{
         return this.last;
      }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onPresentationSlideChange(PresentationSlideChangeEvent event);
   }

   public PresentationSlideChangeEvent(Data data)
   {
      data_ = data;
   }
   
   public RevealSlide getSlide()
   {
      return data_.getSlide();
   }
   
   public int getSlideIndex()
   {
      return data_.getSlideIndex();
   }
   
   public boolean isFirst()
   {
      return data_.isFirst();
   }
   
   public boolean isLast()
   {
      return data_.isLast();
   }
   

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPresentationSlideChange(this);
   }


   public static final Type<Handler> TYPE = new Type<>();
   
   private final Data data_;
}
