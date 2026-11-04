/*
 * UpdateCheckResultTests.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.junit.client.GWTTestCase;

public class UpdateCheckResultTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static final String DESKTOP =
         "{\"status\":\"available\",\"update-version\":\"2026.10.1+239\"," +
         "\"update-url\":\"https://dl.test/RStudio.dmg\",\"update-urgent\":\"0\"," +
         "\"can-install\":true,\"confirm-with-session\":true}";
   private static final String DESKTOP_UP_TO_DATE =
         "{\"status\":\"up-to-date\",\"confirm-with-session\":true}";
   private static final String SESSION =
         "{\"update-version\":\"2026.10.1+239\"," +
         "\"update-url\":\"https://posit.co/download\",\"update-urgent\":\"1\"}";
   private static final String SESSION_NONE =
         "{\"update-version\":\"\",\"update-url\":\"\",\"update-urgent\":\"0\"}";

   public void testReadsTheDesktopFlags()
   {
      UpdateCheckResult desktop = UpdateCheckResult.fromJson(DESKTOP);
      assertTrue(desktop.canInstall());
      assertTrue(desktop.confirmWithSession());
      assertEquals(0, desktop.getUpdateUrgency());

      UpdateCheckResult session = UpdateCheckResult.fromJson(SESSION);
      assertFalse(session.canInstall());
      assertFalse(session.confirmWithSession());
   }

   public void testTheSessionWithdrawsAnUpdate()
   {
      UpdateCheckResult result = UpdateCheckResult.reconcile(
            UpdateCheckResult.fromJson(DESKTOP),
            UpdateCheckResult.fromJson(SESSION_NONE));
      assertEquals("", result.getUpdateVersion());
      assertFalse(result.canInstall());
   }

   public void testTheSessionSetsTheUrgencyOfAnInstallableUpdate()
   {
      UpdateCheckResult result = UpdateCheckResult.reconcile(
            UpdateCheckResult.fromJson(DESKTOP),
            UpdateCheckResult.fromJson(SESSION));
      assertEquals("2026.10.1+239", result.getUpdateVersion());
      assertEquals("https://dl.test/RStudio.dmg", result.getUpdateUrl());
      assertTrue(result.canInstall());
      assertEquals(1, result.getUpdateUrgency());
   }

   public void testTheSessionAloneCanReportAnUpdate()
   {
      UpdateCheckResult result = UpdateCheckResult.reconcile(
            UpdateCheckResult.fromJson(DESKTOP_UP_TO_DATE),
            UpdateCheckResult.fromJson(SESSION));
      assertEquals("2026.10.1+239", result.getUpdateVersion());
      assertEquals("https://posit.co/download", result.getUpdateUrl());
      assertFalse(result.canInstall());
      assertEquals(1, result.getUpdateUrgency());
   }
}
