/*
 * PrefsTests.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.junit.client.GWTTestCase;

public class PrefsTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Regression #18019: during startup the pref layers can momentarily be
   // incomplete (e.g. if a bootstrap RPC fails before they are populated).
   // The index-based accessors (getGlobalValue, getProjectValue, etc.) index
   // directly into layers_, and must tolerate a missing layer by falling back
   // to the default rather than dereferencing an undefined layer and throwing
   // a TypeError (which previously dead-ended quit/startup code paths).

   // userLayer() == 3 and projectLayer() == 4, matching UserPrefs, but the
   // layer array here is empty -- so layers_.get(3) / layers_.get(4) are absent.
   private static class TestPrefs extends Prefs
   {
      public TestPrefs(JsArray<PrefLayer> layers)
      {
         super(layers);
      }

      @Override
      public int userLayer()
      {
         return 3;
      }

      @Override
      public int projectLayer()
      {
         return 4;
      }
   }

   private static native JsArray<PrefLayer> emptyLayers() /*-{
      return [];
   }-*/;

   public void testGetGlobalValueWithIncompleteLayersReturnsDefault()
   {
      TestPrefs prefs = new TestPrefs(emptyLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);
      assertTrue(pref.getGlobalValue());
   }

   public void testGetProjectValueWithIncompleteLayersReturnsDefault()
   {
      TestPrefs prefs = new TestPrefs(emptyLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);
      assertTrue(pref.getProjectValue());
   }

   public void testHasProjectValueWithIncompleteLayersIsFalse()
   {
      TestPrefs prefs = new TestPrefs(emptyLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);
      assertFalse(pref.hasProjectValue());
   }

   public void testSetGlobalValueWithIncompleteLayersDoesNotThrow()
   {
      TestPrefs prefs = new TestPrefs(emptyLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", false);

      // must not throw -- there is no user layer to write to
      pref.setGlobalValue(true, false);

      // the value still reads as its default since nothing was written
      assertFalse(pref.getGlobalValue());
   }
}
