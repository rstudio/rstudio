/*
 * ChatTab.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

import com.google.inject.Inject;

public class ChatTab extends DelayLoadWorkbenchTab<ChatPresenter>
{
   public interface Binder extends CommandBinder<Commands, Shim> {}

   public abstract static class Shim extends DelayLoadTabShim<ChatPresenter, ChatTab>
   {
      @Handler
      public abstract void onActivateChat();

      @Handler
      public abstract void onAssistantPaneToggle();

      @Handler
      public abstract void onUninstallPositAI();
   }

   @Inject
   public ChatTab(Shim shim, Binder binder, Commands commands, Session session)
   {
      super(constants_.chatTitle(), shim);
      shim_ = shim;

      binder.bind(commands, shim_);

      // If chat was popped out in a previous session, force-load the
      // presenter eagerly so it can restore the satellite window even
      // when the sidebar is hidden (bypassing DelayLoad deferral).
      boolean shouldRestore = false;
      try
      {
         JsObject group = session.getSessionInfo().getClientState()
            .peek("chat-window");
         if (group != null)
         {
            JsObject state = group.getObject("chatSatelliteState");
            if (state != null && Boolean.TRUE.equals(state.getBoolean("poppedOut")))
            {
               shouldRestore = true;
            }
         }
      }
      catch (Exception e)
      {
         Debug.log("Failed to read chat satellite state: " + e.getMessage());
      }

      if (shouldRestore)
      {
         shim_.forceLoad(false, null);
      }
   }

   @SuppressWarnings("unused") private final Shim shim_;
   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
