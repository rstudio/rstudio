/*
 * ErrorManagerTests.java
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
package org.rstudio.studio.client.common.debugging;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.ErrorHandlerChangedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserState;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public class ErrorManagerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCreatedAfterSessionInitRestoresHandler()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      session.setSessionInfo(createSessionInfo());
      events.fireEvent(new SessionInitEvent());

      Commands commands = GWT.create(Commands.class);
      ErrorManager manager = new ErrorManager(events, (cmds, handlers) -> () -> {},
            commands, null, session);
      assertEquals(UserState.ERROR_HANDLER_TYPE_TRACEBACK, manager.getErrorHandlerType());
      assertTrue(commands.errorsTraceback().isChecked());
      assertFalse(commands.errorsMessage().isChecked());
   }

   public void testDeferredInitializationKeepsHandlerChangesWorking()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      Commands commands = GWT.create(Commands.class);
      ErrorManager manager = new ErrorManager(events, (cmds, handlers) -> () -> {},
            commands, null, session);

      session.setSessionInfo(createSessionInfo());
      events.fireEvent(new SessionInitEvent());
      assertTrue(commands.errorsTraceback().isChecked());

      events.fireEvent(new ErrorHandlerChangedEvent(createHandlerChange()));
      assertEquals(UserState.ERROR_HANDLER_TYPE_BREAK, manager.getErrorHandlerType());
      assertTrue(commands.errorsBreak().isChecked());
      assertFalse(commands.errorsTraceback().isChecked());
      assertEquals(0, events.getHandlerCount(SessionInitEvent.TYPE));
   }

   private static native SessionInfo createSessionInfo() /*-{
      return { error_state: { error_handler_type: "traceback" } };
   }-*/;

   private static native ErrorHandlerChangedEvent.Data createHandlerChange() /*-{
      return { type: "break" };
   }-*/;
}
