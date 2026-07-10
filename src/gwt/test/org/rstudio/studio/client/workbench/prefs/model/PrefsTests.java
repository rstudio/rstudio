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

   // Mirrors the user/project layer positions used by UserPrefs.
   private static class TestPrefs extends Prefs
   {
      public TestPrefs(JsArray<PrefLayer> layers)
      {
         super(layers);
      }

      @Override
      public int userLayer()
      {
         return UserPrefsAccessor.LAYER_USER;
      }

      @Override
      public int projectLayer()
      {
         return UserPrefsAccessor.LAYER_PROJECT;
      }
   }

   // An empty array -- layers_.get(userLayer()/projectLayer()) is out of bounds,
   // exercising the "index >= layers_.length()" branch of layerValues().
   private static native JsArray<PrefLayer> emptyLayers() /*-{
      return [];
   }-*/;

   // A long-enough array whose user/project slots are null, exercising the
   // "layer == null" branch of layerValues() -- this better models the real
   // bug, where leading layers exist but the user/project layers are not yet
   // populated.
   private static native JsArray<PrefLayer> partialLayers() /*-{
      var layers = [];
      for (var i = 0; i <= @org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor::LAYER_PROJECT; i++)
         layers.push(null);
      return layers;
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

   // The following exercise the "layer == null" branch via partialLayers(),
   // where the array is long enough but the user/project slots are absent.

   public void testGetGlobalValueWithNullLayerReturnsDefault()
   {
      TestPrefs prefs = new TestPrefs(partialLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);
      assertTrue(pref.getGlobalValue());
   }

   public void testGetProjectValueWithNullLayerReturnsDefault()
   {
      TestPrefs prefs = new TestPrefs(partialLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);
      assertTrue(pref.getProjectValue());
   }

   public void testHasProjectValueWithNullLayerIsFalse()
   {
      TestPrefs prefs = new TestPrefs(partialLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);
      assertFalse(pref.hasProjectValue());
   }

   public void testSetProjectValueWithNullLayerDoesNotThrow()
   {
      TestPrefs prefs = new TestPrefs(partialLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", false);

      // must not throw -- there is no project layer to write to
      pref.setProjectValue(true, false);

      // the value still reads as its default since nothing was written
      assertFalse(pref.getProjectValue());
   }

   public void testRemoveGlobalValueWithIncompleteLayersDoesNotThrow()
   {
      TestPrefs prefs = new TestPrefs(partialLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);

      // must not throw -- there is no user layer to remove from
      pref.removeGlobalValue(false);

      assertTrue(pref.getGlobalValue());
   }

   public void testRemoveProjectValueWithIncompleteLayersDoesNotThrow()
   {
      TestPrefs prefs = new TestPrefs(partialLayers());
      Prefs.PrefValue<Boolean> pref = prefs.bool("test", "Test", "Test pref", true);

      // must not throw -- there is no project layer to remove from
      pref.removeProjectValue(false);

      assertTrue(pref.getProjectValue());
   }
}
