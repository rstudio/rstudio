/*
 * ApplicationWindowTests.java
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Provider;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class ApplicationWindowTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testMemoryDismissalSurvivesReloadDespiteNewFigures()
   {
      EventBus events = new EventBus(null, null);
      AriaLiveService aria = new AriaLiveService(events, null)
      {
         @Override
         public boolean isDisabled(String id)
         {
            return true;
         }
      };
      int[] created = {0};
      Provider<WarningBar> provider = () ->
      {
         created[0]++;
         return new WarningBar(events);
      };
      StudioClientApplicationConstants constants = GWT.create(StudioClientApplicationConstants.class);
      WarningBarList previous = new WarningBarList(provider, aria, () -> {}, () -> {}, () -> {});
      previous.show(
            "memory-limit",
            "approaching",
            true,
            constants.approachingMemoryLimit() + " 90 of 100 MB",
            false,
            null,
            null);
      previous.dismiss("memory-limit");

      Session session = new Session(events);
      session.setSessionInfo(sessionInfo(previous.getDismissedState()));
      created[0] = 0;
      ApplicationWindow window = new ApplicationWindow(
            new TestHeader(),
            null,
            null,
            events,
            provider,
            session,
            aria,
            null,
            null);

      // A reload starts with updateOnly=false, and the figures have moved on.
      window.showMemoryLimitWarning("95 of 100 MB", false, false);
      assertEquals(0, created[0]);
      window.showWarning(false, "Unrelated warning");
      window.showMemoryLimitWarning("96 of 100 MB", true, false);
      assertEquals(1, created[0]);

      // Going over the limit is a new warning.
      window.showMemoryLimitWarning("110 of 100 MB", false, true);
      assertEquals(2, created[0]);
   }

   public void testEmptyWarningLayerDoesNotCoverWorkbench()
   {
      EventBus events = new EventBus(null, null);
      AriaLiveService aria = new AriaLiveService(events, null)
      {
         @Override
         public boolean isDisabled(String id)
         {
            return true;
         }
      };
      ApplicationWindow window = new ApplicationWindow(
            new TestHeader(),
            null,
            null,
            events,
            () -> new WarningBar(events),
            new Session(events),
            aria,
            null,
            null);
      window.showWorkbenchView(new SimplePanel());

      // The warnings layer follows the workbench in the DOM, so it paints over
      // the workbench wherever it is displayed.
      Element found = null;
      for (Element candidate = window.getElement().getFirstChildElement();
           candidate != null;
           candidate = candidate.getNextSiblingElement())
      {
         Element child = candidate.getFirstChildElement();
         if (child != null && "contentinfo".equals(child.getAttribute("role")))
            found = candidate;
      }
      assertNotNull(found);
      Element layer = found;

      // LayoutPanel applies layer styles in a scheduled layout pass.
      delayTestFinish(5000);
      Scheduler.get().scheduleDeferred(() ->
      {
         assertEquals("none", layer.getStyle().getDisplay());

         window.showWarning("example", false, "Example warning", null, null);
         assertEquals("", layer.getStyle().getDisplay());

         window.hideWarning("example");
         assertEquals("none", layer.getStyle().getDisplay());
         finishTest();
      });
   }

   private static native SessionInfo sessionInfo(String dismissed) /*-{
      return {client_state: {applicationWarnings: {dismissed: dismissed}}};
   }-*/;

   private static class TestHeader extends SimplePanel implements ApplicationHeader
   {
      public int getPreferredHeight() { return 0; }
      public void showToolbar(boolean show) {}
      public boolean isToolbarVisible() { return false; }
      public void focusToolbar() {}
      public void focusGoToFunction() {}
   }
}
