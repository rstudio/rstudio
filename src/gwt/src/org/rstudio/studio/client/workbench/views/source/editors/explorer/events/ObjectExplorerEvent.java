/*
 * ObjectExplorerEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer.events;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ObjectExplorerEvent extends GwtEvent<ObjectExplorerEvent.Handler>
{
   // NOTE: these should be synchronized with the 'explorer.types' variable
   // defined in 'SessionObjectExplorer.R'
   public enum EventType
   {
      // unknown event type (not normally used)
      UNKNOWN,
      
      // create a new object explorer tab
      NEW,
      
      // open a node
      OPEN_NODE,
      
      // close a node
      CLOSE_NODE
   }
   
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public final native String getType()
      /*-{
         return this.type;
      }-*/;
      
      public final native ObjectExplorerHandle getHandle()
      /*-{
         return this.data;
      }-*/;
   }
   
   public ObjectExplorerEvent(Data data)
   {
      data_ = data;
      type_ = valueOf(data.getType());
   }
   
   public Data getData()
   {
      return data_;
   }
   
   public EventType getType()
   {
      return type_;
   }
   
   private EventType valueOf(String value)
   {
      EventType type = EventType.UNKNOWN;
      
      try
      {
         type = EventType.valueOf(value.toUpperCase());
      }
      catch (Exception e)
      {
         Debug.logException(e);
      }
      
      return type;
   }
   
   private final Data data_;
   private final EventType type_;
   
   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onObjectExplorerEvent(ObjectExplorerEvent event);
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onObjectExplorerEvent(this);
   }
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
