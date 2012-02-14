/*
 * CompilePdfStatusEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.compilepdf.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CompilePdfStatusEvent extends GwtEvent<CompilePdfStatusEvent.Handler>
{
   public static final int STARTED = 0;
   public static final int COMPLETED = 1;
   
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public native final int getStatus() /*-{
         return this.status;
      }-*/;

      public native final String getText() /*-{
         return this.text;
      }-*/;
   }
   
   public interface Handler extends EventHandler
   {
      void onCompilePdfStatus(CompilePdfStatusEvent event);
   }

   public CompilePdfStatusEvent(Data data)
   {
      data_ = data;
   }

   public int getStatus()
   {
      return data_.getStatus();
   }
   
   public String getText()
   {
      return data_.getText();
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCompilePdfStatus(this);
   }
   
   private final Data data_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
