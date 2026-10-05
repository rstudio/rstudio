/*
 * WorkbenchListManagerTests.java
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
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.junit.client.GWTTestCase;

public class WorkbenchListManagerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCreatedAfterSessionInitPopulatesLists()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      session.setSessionInfo(createSessionInfo());
      events.fireEvent(new SessionInitEvent());

      WorkbenchListManager manager = new WorkbenchListManager(events, session, null);
      List<ArrayList<String>> received = new ArrayList<>();
      manager.getFileMruList().addListChangedHandler(event -> received.add(event.getList()));

      assertEquals(1, received.size());
      assertEquals(Arrays.asList("startup.R"), received.get(0));
   }

   public void testDeferredInitializationPreservesLaterListUpdates()
   {
      EventBus events = new EventBus(null, null);
      Session session = new Session(events);
      WorkbenchListManager manager = new WorkbenchListManager(events, session, null);
      List<ArrayList<String>> received = new ArrayList<>();
      manager.getFileMruList().addListChangedHandler(event -> received.add(event.getList()));
      assertEquals(0, received.size());

      session.setSessionInfo(createSessionInfo());
      events.fireEvent(new SessionInitEvent());
      assertEquals(1, received.size());
      assertEquals(Arrays.asList("startup.R"), received.get(0));

      ArrayList<String> updated = new ArrayList<>(Arrays.asList("updated.R"));
      events.fireEvent(new ListChangedEvent("file_mru", updated));
      events.fireEvent(new SessionInitEvent());
      assertEquals(2, received.size());
      assertEquals(updated, received.get(1));

      List<ArrayList<String>> lateSubscriber = new ArrayList<>();
      manager.getFileMruList().addListChangedHandler(event -> lateSubscriber.add(event.getList()));
      assertEquals(updated, lateSubscriber.get(0));
   }

   private static native SessionInfo createSessionInfo() /*-{
      return { lists: {
         file_mru: ["startup.R"],
         project_name_mru: [],
         plot_publish_mru: [],
         command_palette_mru: [],
         help_history_links: [],
         user_dictionary: []
      }};
   }-*/;
}
