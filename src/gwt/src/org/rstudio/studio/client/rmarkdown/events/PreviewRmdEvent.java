/*
 * PreviewRmdEvent.java
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

package org.rstudio.studio.client.rmarkdown.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PreviewRmdEvent extends GwtEvent<PreviewRmdEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getSourceFile() /*-{
         return this.source_file;
      }-*/;

      public final native String getEncoding() /*-{
         return this.encoding;
      }-*/;

      public final native String getOutputFile() /*-{
         return this.output_file;
      }-*/;
   }


   public interface Handler extends EventHandler
   {
      void onPreviewRmd(PreviewRmdEvent event);
   }

   public PreviewRmdEvent(Data data)
   {
      data_ = data;
   }

   public String getSourceFile()
   {
      return data_.getSourceFile();
   }

   public String getEncoding()
   {
      return data_.getEncoding();
   }

   public String getOutputFile()
   {
      return data_.getOutputFile();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPreviewRmd(this);
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
