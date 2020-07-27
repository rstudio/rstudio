/*
 * AceKeyboardActivityEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AceKeyboardActivityEvent extends GwtEvent<AceKeyboardActivityEvent.Handler>
{
   public AceKeyboardActivityEvent()
   {
      this(nullEvent());
   }

   public AceKeyboardActivityEvent(JavaScriptObject event)
   {
      event_ = event;
   }

   public JavaScriptObject getCommandData()
   {
      return event_;
   }

   public boolean isChainEvent()
   {
      return isChainEvent(event_);
   }

   private static final native boolean isChainEvent(JavaScriptObject event) /*-{
      if (event == null)
         return true;

      var command = event.command;
      return command === "null" || command === "chainKeys";
   }-*/;

   private final JavaScriptObject event_;

   private static native final JavaScriptObject nullEvent() /*-{
      return {command: "null"};
   }-*/;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onAceKeyboardActivity(AceKeyboardActivityEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAceKeyboardActivity(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
