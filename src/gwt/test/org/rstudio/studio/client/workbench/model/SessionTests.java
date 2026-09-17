/*
 * SessionTests.java
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
package org.rstudio.studio.client.workbench.model;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

public class SessionTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testAvailableSessionInfoRunsSynchronously()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      SessionInfo info = JavaScriptObject.createObject().cast();
      session.setSessionInfo(info);

      List<SessionInfo> received = new ArrayList<>();
      session.withSessionInfo(received::add);
      assertEquals(1, received.size());
      assertSame(info, received.get(0));
      assertEquals(0, events.getHandlerCount(SessionInitEvent.TYPE));

      events.fireEvent(new SessionInitEvent());
      assertEquals(1, received.size());
   }

   public void testWaitsForSessionInitAndUnregisters()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      List<SessionInfo> received = new ArrayList<>();
      session.withSessionInfo(received::add);
      session.withSessionInfo(received::add);
      assertEquals(0, received.size());

      SessionInfo info = JavaScriptObject.createObject().cast();
      session.setSessionInfo(info);
      assertEquals(0, received.size());

      events.fireEvent(new SessionInitEvent());
      assertEquals(2, received.size());
      assertSame(info, received.get(0));
      assertSame(info, received.get(1));
      assertEquals(0, events.getHandlerCount(SessionInitEvent.TYPE));

      events.fireEvent(new SessionInitEvent());
      assertEquals(2, received.size());
   }
}
