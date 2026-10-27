/*
 * AssistantKeyForwarder.java
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
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.core.client.command.ShortcutManager;

import com.google.gwt.dom.client.NativeEvent;

// Exposes $wnd.rstudioChatKeydown to the Posit Assistant iframe, whose caller is
// packages/rstudio/src/client/rstudioShortcutForwarder.ts in posit-dev/assistant.
// The call must stay synchronous: on Desktop the key has to be cancelled before
// the iframe's keydown dispatch ends, or Electron's menu runs the command again.
public class AssistantKeyForwarder
{
   public static native void install() /*-{
      $wnd.rstudioChatKeydown = $entry(function(e) {
         return @org.rstudio.studio.client.workbench.views.chat.AssistantKeyForwarder::onKeyDown(*)(e);
      });
   }-*/;

   private static boolean onKeyDown(NativeEvent event)
   {
      if (!ShortcutManager.INSTANCE.onAssistantKeyDown(event))
         return false;

      event.preventDefault();
      event.stopPropagation();
      return true;
   }
}
