/*
 * RprofEvent.java
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

package org.rstudio.studio.client.workbench.views.source.editors.profiler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RprofEvent extends GwtEvent<RprofEvent.Handler>
{
   public enum RprofEventType
   {
      START,
      STOP,
      CREATE
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getPath() /*-{
         return this.path;
      }-*/;

      public final native String getHtmlPath() /*-{
         return this.htmlPath;
      }-*/;

      public final native String getHtmlLocalPath() /*-{
         return this.htmlLocalPath;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onRprofEvent(RprofEvent event);
   }

   public RprofEvent(RprofEventType type, RprofEvent.Data data)
   {
      type_ = type;
      data_ = data;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRprofEvent(this);
   }

   public RprofEventType getEventType()
   {
      return type_;
   }

   public RprofEvent.Data getData()
   {
      return data_;
   }

   public static final Type<Handler> TYPE = new Type<>();
   private RprofEventType type_;
   private RprofEvent.Data data_;
}
