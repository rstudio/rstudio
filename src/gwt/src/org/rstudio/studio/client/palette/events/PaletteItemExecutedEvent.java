/*
 * PaletteItemExecutedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.palette.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.palette.model.CommandPaletteMruEntry;

/**
 * An event emitted to the global event bus after a command palette item has been executed.
 */
public class PaletteItemExecutedEvent
   extends GwtEvent<PaletteItemExecutedEvent.Handler>
{
   public PaletteItemExecutedEvent(String scope, String id)
   {
      entry_ = new CommandPaletteMruEntry(scope, id);
   }

   public CommandPaletteMruEntry getMruEntry()
   {
      return entry_;
   }

   public interface Handler extends EventHandler
   {
      void onPaletteItemExecuted(PaletteItemExecutedEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPaletteItemExecuted(this);
   }

   private final CommandPaletteMruEntry entry_;

   public static final Type<Handler> TYPE = new Type<>();
}
