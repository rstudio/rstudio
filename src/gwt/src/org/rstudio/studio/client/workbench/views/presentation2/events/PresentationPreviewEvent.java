/*
 * PresentationPreviewEvent.java
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


import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocation;
import org.rstudio.studio.client.quarto.model.QuartoNavigate;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PresentationPreviewEvent extends GwtEvent<PresentationPreviewEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public final native String getUrl() /*-{
         return this.url;
      }-*/;


      public final native QuartoNavigate getQuartoNavigation() /*-{
         return this.quarto_navigation;
      }-*/;
      
      public final native PresentationEditorLocation getEditorState() /*-{
         return this.editor_state;
      }-*/;
      
      public final native int getSlideLevel()  /*-{
         return this.slide_level;
      }-*/;
   }
   
   
   public interface Handler extends EventHandler
   {
      void onPresentationPreview(PresentationPreviewEvent event);
   }

   public PresentationPreviewEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPresentationPreview(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
