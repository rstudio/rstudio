/*
 * ProcessExitEvent.java
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.common.console.ProcessExitEvent.Handler;

public class ProcessExitEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onProcessExit(ProcessExitEvent event);
   }

   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addProcessExitHandler(Handler handler);
   }

   public ProcessExitEvent(int exitCode)
   {
      exitCode_ = exitCode;
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
      handler.onProcessExit(this);
   }

   private final int exitCode_;

   public static final Type<Handler> TYPE = new Type<>();
}
