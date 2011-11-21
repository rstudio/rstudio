/*
 * PosixShellOutputEvent.java
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
import org.rstudio.studio.client.common.posixshell.events.PosixShellOutputEvent.Handler;

public class PosixShellOutputEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onPosixShellOutput(PosixShellOutputEvent event);
   }

   public PosixShellOutputEvent(String output)
   {
      output_ = output;
   }

   public String getOutput()
   {
      return output_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPosixShellOutput(this);
   }

   private final String output_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
