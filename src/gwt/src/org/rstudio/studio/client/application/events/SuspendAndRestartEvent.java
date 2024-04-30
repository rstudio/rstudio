/*
 * SuspendAndRestartEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.application.events;

import org.rstudio.studio.client.application.model.SuspendOptions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SuspendAndRestartEvent extends GwtEvent<SuspendAndRestartEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final SuspendOptions getOptions() /*-{
         return this.options;
      }-*/;
   }

   public SuspendAndRestartEvent(Data data)
   {
      this(data.getOptions());
   }

   public SuspendAndRestartEvent(SuspendOptions suspendOptions)
   {
      suspendOptions_ = suspendOptions;
   }

   public SuspendAndRestartEvent(String afterRestartCommand)
   {
      this(SuspendOptions.createSaveAll(false, afterRestartCommand));
   }

   public SuspendOptions getSuspendOptions()
   {
      return suspendOptions_;
   }

   private final SuspendOptions suspendOptions_;

   // Boilerplate ----
   
   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSuspendAndRestart(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public interface Handler extends EventHandler
   {
      void onSuspendAndRestart(SuspendAndRestartEvent event);
   }
}
