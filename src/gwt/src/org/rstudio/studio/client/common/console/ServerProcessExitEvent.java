/*
 * ServerProcessExitEvent.java
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
package org.rstudio.studio.client.common.console;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ServerProcessExitEvent extends GwtEvent<ServerProcessExitEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onServerProcessExit(ServerProcessExitEvent event);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final String getHandle() /*-{ return this.handle; }-*/;
      public native final int getExitCode() /*-{ return this.exitCode; }-*/;
   }

   public ServerProcessExitEvent(String procHandle,
                                 int exitCode)
   {
      procHandle_ = procHandle;
      exitCode_ = exitCode;
   }

   public String getProcessHandle()
   {
      return procHandle_;
   }

   public int getExitCode()
   {
      return exitCode_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onServerProcessExit(this);
   }

   private final String procHandle_;
   private final int exitCode_;

   public static final Type<Handler> TYPE = new Type<>();
}
