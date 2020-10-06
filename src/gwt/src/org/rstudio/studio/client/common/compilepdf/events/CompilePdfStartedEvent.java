/*
 * CompilePdfStartedEvent.java
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
package org.rstudio.studio.client.common.compilepdf.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CompilePdfStartedEvent extends GwtEvent<CompilePdfStartedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getTargetFile() /*-{
         return this.target_file;
      }-*/;

      public final native String getPdfPath() /*-{
         return this.pdf_path;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onCompilePdfStarted(CompilePdfStartedEvent event);
   }

   public CompilePdfStartedEvent(Data data)
   {
      data_ = data;
   }

   public String getTargetFile()
   {
      return data_.getTargetFile();
   }

   public String getPdfPath()
   {
      return data_.getPdfPath();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCompilePdfStarted(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
