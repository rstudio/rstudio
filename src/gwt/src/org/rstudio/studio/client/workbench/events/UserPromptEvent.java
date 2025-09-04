/*
 * UserPromptEvent.java
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

package org.rstudio.studio.client.workbench.events;

import org.rstudio.studio.client.workbench.model.UserPrompt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class UserPromptEvent extends GwtEvent<UserPromptEvent.Handler>
{
   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   public interface Handler extends EventHandler
   {
      void onUserPrompt(UserPromptEvent event);
   }

   public UserPromptEvent(UserPrompt userPrompt)
   {
      userPrompt_ = userPrompt;
   }

   public UserPrompt getUserPrompt()
   {
      return userPrompt_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onUserPrompt(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final UserPrompt userPrompt_;
}
