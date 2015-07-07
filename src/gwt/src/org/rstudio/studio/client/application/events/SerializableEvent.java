/*
 * SerializableEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.Debug;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public abstract class SerializableEvent<T, 
       H extends EventHandler> extends GwtEvent<H>
{
   public SerializableEvent(EventSerializer<T> serializer)
   {
      serializer_ = serializer;
   }

   public JavaScriptObject serializeToJSO(GwtEvent<?> event)
   {
      Debug.devlog("serializing");
      Debug.logObject(serializer_.serializeToJSO(event));
      return serializer_.serializeToJSO(event);
   }

   public T deserializeFromJSO(JavaScriptObject jso)
   {
      return serializer_.deserializeFromJSO(jso);
   }
   
   private final EventSerializer<T> serializer_;
}
