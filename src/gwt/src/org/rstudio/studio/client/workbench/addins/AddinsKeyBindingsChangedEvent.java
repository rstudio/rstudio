/*
 * AddinsKeyBindingsChangedEvent.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from Addins pursuant
 * to the terms of a commercial license agreement with Addins, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.addins;

import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class AddinsKeyBindingsChangedEvent extends CrossWindowEvent<AddinsKeyBindingsChangedEvent.Handler>
{
   // Default method is required by @JavaScriptSerializable annotation
   public AddinsKeyBindingsChangedEvent()
   {
      bindings_ = JavaScriptObject.createObject();
   }

   public AddinsKeyBindingsChangedEvent(EditorKeyBindings bindings)
   {
      bindings_ = bindings.cast();
   }

   public EditorKeyBindings getBindings()
   {
      return bindings_.cast();
   }

   private final JavaScriptObject bindings_;

   // Boilerplate ----

   @Override
   public boolean forward() { return false; }

   public interface Handler extends EventHandler
   {
      void onAddinsKeyBindingsChanged(AddinsKeyBindingsChangedEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAddinsKeyBindingsChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
