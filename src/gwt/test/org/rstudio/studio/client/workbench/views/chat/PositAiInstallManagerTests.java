/*
 * PositAiInstallManagerTests.java
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

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Exercises PositAiInstallManager.dispatchUpdateCheck -- the pure interpretation
 * of a chatCheckForUpdates response. The @ai end-to-end tests that would
 * otherwise cover this do not run in ordinary PR CI, so these guard the parsing
 * directly: malformed payloads must route to onCheckFailed (not be treated as
 * "up to date"), optional flags must default to false, and exactly one callback
 * must fire on every path.
 */
public class PositAiInstallManagerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Records which UpdateCheckCallback method fired (and the args tests assert
   // on) plus how many fired, so each dispatch can be checked for exactly one.
   private static class Recorder implements PositAiInstallManager.UpdateCheckCallback
   {
      int calls = 0;
      String method = null;
      boolean isInitialInstall = false;
      boolean isDowngrade = false;
      boolean additionalProvidersAvailable = false;
      String currentVersion = null;
      String newVersion = null;

      @Override
      public void onNoUpdateAvailable()
      {
         calls++;
         method = "onNoUpdateAvailable";
      }

      @Override
      public void onUpdateAvailable(String currentVersion, String newVersion,
                                    boolean isInitialInstall, boolean isDowngrade,
                                    boolean additionalProvidersAvailable)
      {
         calls++;
         method = "onUpdateAvailable";
         this.currentVersion = currentVersion;
         this.newVersion = newVersion;
         this.isInitialInstall = isInitialInstall;
         this.isDowngrade = isDowngrade;
         this.additionalProvidersAvailable = additionalProvidersAvailable;
      }

      @Override
      public void onIncompatibleVersion()
      {
         calls++;
         method = "onIncompatibleVersion";
      }

      @Override
      public void onUnsupportedVersionUpgradeRequired(String currentVersion,
                                                      String newVersion,
                                                      boolean isDowngrade)
      {
         calls++;
         method = "onUnsupportedVersionUpgradeRequired";
         this.currentVersion = currentVersion;
         this.newVersion = newVersion;
         this.isDowngrade = isDowngrade;
      }

      @Override
      public void onUnsupportedVersionNoUpdate(String currentVersion)
      {
         calls++;
         method = "onUnsupportedVersionNoUpdate";
         this.currentVersion = currentVersion;
      }

      @Override
      public void onUnsupportedProtocol()
      {
         calls++;
         method = "onUnsupportedProtocol";
      }

      @Override
      public void onManifestUnavailable(String errorMessage)
      {
         calls++;
         method = "onManifestUnavailable";
      }

      @Override
      public void onCheckFailed(String errorMessage)
      {
         calls++;
         method = "onCheckFailed";
      }
   }

   private static JsObject emptyObject()
   {
      return JavaScriptObject.createObject().cast();
   }

   // A well-formed response with every required flag present and false.
   private static JsObject wellFormed()
   {
      JsObject o = emptyObject();
      o.setBoolean("manifestUnavailable", false);
      o.setBoolean("unsupportedProtocol", false);
      o.setBoolean("noCompatibleVersion", false);
      o.setBoolean("unsupportedInstalledVersion", false);
      o.setBoolean("updateAvailable", false);
      o.setBoolean("isInitialInstall", false);
      return o;
   }

   public void testNullResultRoutesToCheckFailed()
   {
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(null, r);
      assertEquals(1, r.calls);
      assertEquals("onCheckFailed", r.method);
   }

   public void testMissingRequiredFlagRoutesToCheckFailed()
   {
      JsObject o = wellFormed();
      o.unset("updateAvailable");
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onCheckFailed", r.method);
   }

   public void testNonBooleanRequiredFlagRoutesToCheckFailed()
   {
      JsObject o = wellFormed();
      o.setString("updateAvailable", "true"); // string, not boolean
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onCheckFailed", r.method);
   }

   public void testAllRequiredFalseRoutesToNoUpdate()
   {
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(wellFormed(), r);
      assertEquals(1, r.calls);
      assertEquals("onNoUpdateAvailable", r.method);
   }

   public void testUpdateAvailableDefaultsOptionalFlagsToFalse()
   {
      JsObject o = wellFormed();
      o.setBoolean("updateAvailable", true);
      o.setBoolean("isInitialInstall", true);
      o.setString("currentVersion", "0.0.0");
      o.setString("newVersion", "1.2.3");
      // isDowngrade and additionalProvidersAvailable intentionally omitted
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onUpdateAvailable", r.method);
      assertEquals("1.2.3", r.newVersion);
      assertTrue(r.isInitialInstall);
      assertFalse(r.isDowngrade);
      assertFalse(r.additionalProvidersAvailable);
   }

   public void testManifestUnavailableTakesPrecedenceOverUpdateAvailable()
   {
      JsObject o = wellFormed();
      o.setBoolean("manifestUnavailable", true);
      o.setBoolean("updateAvailable", true); // must be ignored
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onManifestUnavailable", r.method);
   }

   public void testUnsupportedProtocolRoutesToUnsupportedProtocol()
   {
      JsObject o = wellFormed();
      o.setBoolean("unsupportedProtocol", true);
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onUnsupportedProtocol", r.method);
   }

   public void testNoCompatibleVersionRoutesToIncompatibleVersion()
   {
      JsObject o = wellFormed();
      o.setBoolean("noCompatibleVersion", true);
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onIncompatibleVersion", r.method);
   }

   public void testUnsupportedInstalledVersionWithUpdateRoutesToUpgradeRequired()
   {
      JsObject o = wellFormed();
      o.setBoolean("unsupportedInstalledVersion", true);
      o.setBoolean("updateAvailable", true);
      o.setString("currentVersion", "1.0.0");
      o.setString("newVersion", "2.0.0");
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onUnsupportedVersionUpgradeRequired", r.method);
      assertEquals("2.0.0", r.newVersion);
   }

   public void testUnsupportedInstalledVersionWithoutUpdateRoutesToNoUpdate()
   {
      JsObject o = wellFormed();
      o.setBoolean("unsupportedInstalledVersion", true);
      o.setString("currentVersion", "1.0.0");
      Recorder r = new Recorder();
      PositAiInstallManager.dispatchUpdateCheck(o, r);
      assertEquals(1, r.calls);
      assertEquals("onUnsupportedVersionNoUpdate", r.method);
      assertEquals("1.0.0", r.currentVersion);
   }
}
