/*
 * BuildErrorsEvent.java
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
package org.rstudio.studio.client.workbench.views.buildtools.events;

import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildErrorsEvent extends GwtEvent<BuildErrorsEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getBaseDirectory() /*-{
         return this.base_dir;
      }-*/;

      public final native JsArray<SourceMarker> getErrors() /*-{
         return this.errors;
      }-*/;

      public final native boolean openErrorList() /*-{
         return this.open_error_list;
      }-*/;

      public final native String type() /*-{
         return this.type;
      }-*/;
   }


   public interface Handler extends EventHandler
   {
      void onBuildErrors(BuildErrorsEvent event);
   }

   public BuildErrorsEvent(Data data)
   {
      data_ = data;
   }

   public String getBaseDirectory()
   {
      return data_.getBaseDirectory();
   }

   public boolean openErrorList()
   {
      return data_.openErrorList();
   }

   public String type()
   {
      return data_.type();
   }

   public JsArray<SourceMarker> getErrors()
   {
      return data_.getErrors();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onBuildErrors(this);
   }

   private Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
