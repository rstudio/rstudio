/*
 * PosixShellExitEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.posixshell.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.common.posixshell.events.PosixShellExitEvent.Handler;

public class PosixShellExitEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onPosixShellExit(PosixShellExitEvent event);
   }

   public PosixShellExitEvent(int exitCode)
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
      handler.onPosixShellExit(this);
   }

   private final int exitCode_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
