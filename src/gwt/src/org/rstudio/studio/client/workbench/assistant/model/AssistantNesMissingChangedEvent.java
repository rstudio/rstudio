/*
 * AssistantNesMissingChangedEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.assistant.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// Fired when the Posit Assistant installation gains or loses NES, e.g. after
// an install or update.
public class AssistantNesMissingChangedEvent extends GwtEvent<AssistantNesMissingChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAssistantNesMissingChanged(AssistantNesMissingChangedEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final boolean getNesMissing() /*-{
         return this.nes_missing;
      }-*/;
   }

   public AssistantNesMissingChangedEvent(boolean nesMissing)
   {
      nesMissing_ = nesMissing;
   }

   public boolean isNesMissing()
   {
      return nesMissing_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAssistantNesMissingChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final boolean nesMissing_;
}
