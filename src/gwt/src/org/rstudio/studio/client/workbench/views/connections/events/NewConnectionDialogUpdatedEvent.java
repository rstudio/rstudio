/*
 * NewConnectionDialogUpdatedEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.connections.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class NewConnectionDialogUpdatedEvent extends GwtEvent<NewConnectionDialogUpdatedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {  
      }
      
      public final native String getCode() /*-{
         return this.code;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onNewConnectionDialogUpdated(NewConnectionDialogUpdatedEvent event);
   }

   public NewConnectionDialogUpdatedEvent(Data data)
   {
      data_ = data;
   }
   
   public String getCode()
   {
      return data_.getCode();
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNewConnectionDialogUpdated(this);
   }
   
   private final Data data_;
  
   public static final Type<Handler> TYPE = new Type<Handler>();
}