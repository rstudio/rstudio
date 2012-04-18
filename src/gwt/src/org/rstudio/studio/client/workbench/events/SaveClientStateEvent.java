/*
 * SaveClientStateEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.model.ClientState;

public class SaveClientStateEvent extends GwtEvent<SaveClientStateHandler>
{
   public static final GwtEvent.Type<SaveClientStateHandler> TYPE =
      new GwtEvent.Type<SaveClientStateHandler>();

   public SaveClientStateEvent()
   {
      this(ClientState.create());
   }

   public SaveClientStateEvent(ClientState state)
   {
      state_ = state;
   }

   public ClientState getState()
   {
      return state_;
   }

   @Override
   protected void dispatch(SaveClientStateHandler handler)
   {
      handler.onSaveClientState(this);
   }

   @Override
   public GwtEvent.Type<SaveClientStateHandler> getAssociatedType()
   {
      return TYPE;
   }

   private final ClientState state_;
}
