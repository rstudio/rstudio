/*
 * WorkbenchWarningTests.java
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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DummyGlobalDisplay;
import org.rstudio.studio.client.workbench.commands.DummyCommands;
import org.rstudio.studio.client.workbench.events.QuotaStatusEvent;
import org.rstudio.studio.client.workbench.model.QuotaStatus;

public class WorkbenchWarningTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testOverToNearQuotaReplacesOverQuotaWarning()
   {
      RecordingDisplay display = new RecordingDisplay();
      Workbench workbench = createWorkbench(display);
      display.messages_.put("other", "Other warning");
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(95)));
      String nearMessage = display.messages_.get("quota");
      assertNotNull(nearMessage);
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(96)));
      assertEquals(nearMessage, display.messages_.get("quota"));
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(105)));
      assertFalse(nearMessage.equals(display.messages_.get("quota")));

      // Still near quota, so the near-quota warning returns, as it would if
      // it had never been shown.
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(95)));
      assertEquals(nearMessage, display.messages_.get("quota"));
      assertEquals("Other warning", display.messages_.get("other"));
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(96)));
      assertEquals(nearMessage, display.messages_.get("quota"));
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(80)));
      assertFalse(display.messages_.containsKey("quota"));
   }

   public void testFirstNearQuotaWarningReplacesOverQuotaWarning()
   {
      RecordingDisplay display = new RecordingDisplay();
      Workbench workbench = createWorkbench(display);
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(105)));
      String overMessage = display.messages_.get("quota");
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(95)));
      assertNotNull(display.messages_.get("quota"));
      assertFalse(overMessage.equals(display.messages_.get("quota")));
      workbench.onQuotaStatus(new QuotaStatusEvent(quota(80)));
      assertFalse(display.messages_.containsKey("quota"));
   }

   private static native QuotaStatus quota(int used) /*-{
      return {used: used, quota: 100, limit: 120};
   }-*/;

   private static Workbench createWorkbench(RecordingDisplay display)
   {
      return new Workbench(
            null,
            null,
            display,
            GWT.create(DummyCommands.class),
            new EventBus(null, null),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
   }

   private static class RecordingDisplay extends DummyGlobalDisplay
   {
      @Override
      public void showWarningBar(String id, boolean severe, String message, String label, Command action)
      {
         messages_.put(id, message);
      }

      @Override
      public void hideWarningBar(String id)
      {
         messages_.remove(id);
      }

      private final Map<String, String> messages_ = new HashMap<>();
   }
}
