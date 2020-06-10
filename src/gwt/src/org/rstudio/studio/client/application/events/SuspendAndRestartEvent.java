/*
 * SuspendAndRestartEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.application.model.SuspendOptions;

import com.google.gwt.core.client.JavaScriptObject;
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

      public native final String getAfterRestartCommand() /*-{
         return this.after_restart;
      }-*/;
   }

   public SuspendAndRestartEvent(Data data)
   {
      this(data.getOptions(), data.getAfterRestartCommand());
   }

   public SuspendAndRestartEvent(SuspendOptions suspendOptions,
                                 String afterRestartCommand)
   {
      if (suspendOptions == null)
         suspendOptions = SuspendOptions.createSaveAll(false);
      suspendOptions_ = suspendOptions;
      afterRestartCommand_ = afterRestartCommand;
   }

   public SuspendAndRestartEvent(String afterRestartCommand)
   {
      this(null, afterRestartCommand);
   }

   public SuspendOptions getSuspendOptions()
   {
      return suspendOptions_;
   }

   public String getAfterRestartCommand()
   {
      return afterRestartCommand_;
   }

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

   private final SuspendOptions suspendOptions_;
   private final String afterRestartCommand_;

   public interface Handler extends EventHandler
   {
      void onSuspendAndRestart(SuspendAndRestartEvent event);
   }
}
