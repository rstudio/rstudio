/*
 * QuitAbortedEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * The session acknowledged a quit but R then refused to quit (.Last() failed,
 * or the workspace could not be saved), so the session goes on.
 */
public class QuitAbortedEvent extends GwtEvent<QuitAbortedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onQuitAborted(QuitAbortedEvent event);
   }

   public static final Type<Handler> TYPE = new Type<>();

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onQuitAborted(this);
   }
}
