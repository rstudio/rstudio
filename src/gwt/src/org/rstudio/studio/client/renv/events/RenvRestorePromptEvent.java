/*
 * RenvRestorePromptEvent.java
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
package org.rstudio.studio.client.renv.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when the renv project library is empty and packages could be
 * restored from the lockfile.
 */
public class RenvRestorePromptEvent extends GwtEvent<RenvRestorePromptEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onRenvRestorePrompt(RenvRestorePromptEvent event);
   }

   public RenvRestorePromptEvent()
   {
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRenvRestorePrompt(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public static final Type<Handler> TYPE = new Type<>();
}
