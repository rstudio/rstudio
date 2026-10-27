/*
 * EmbeddedKeyRoutingTests.java
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
package org.rstudio.core.client.command;

import com.google.gwt.junit.client.GWTTestCase;

public class EmbeddedKeyRoutingTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testWorkbenchLevelContextsAllowed()
   {
      assertTrue(AppCommand.Context.Workbench.isWorkbenchLevel());
      assertTrue(AppCommand.Context.Addin.isWorkbenchLevel());
      assertTrue(AppCommand.Context.PackageDevelopment.isWorkbenchLevel());
   }

   public void testEditorAndLanguageContextsNotAllowed()
   {
      assertFalse(AppCommand.Context.Editor.isWorkbenchLevel());
      assertFalse(AppCommand.Context.R.isWorkbenchLevel());
      assertFalse(AppCommand.Context.RMarkdown.isWorkbenchLevel());
      assertFalse(AppCommand.Context.Cpp.isWorkbenchLevel());
   }

   public void testMainModeRoutedOutOfSatellite()
   {
      assertTrue(AppCommand.isRoutedOutOfSatellite("main", "sat"));
   }

   public void testBackgroundModeRoutedOutOfSatellite()
   {
      assertTrue(AppCommand.isRoutedOutOfSatellite("background", "sat"));
   }

   public void testAnyModeStaysInSatellite()
   {
      assertFalse(AppCommand.isRoutedOutOfSatellite("any", "sat"));
   }

   public void testOwnSatelliteModeStaysInSatellite()
   {
      assertFalse(AppCommand.isRoutedOutOfSatellite("sat", "sat"));
   }
}
