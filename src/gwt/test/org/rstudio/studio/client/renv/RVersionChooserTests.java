/*
 * RVersionChooserTests.java
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
package org.rstudio.studio.client.renv;

import com.google.gwt.junit.client.GWTTestCase;

// The installations listed name bin\R.exe, while the desktop keeps and
// reports bin\x64\Rterm.exe, with its separators as it found them; both
// must be taken as the same R on Windows, and only identical paths elsewhere.
public class RVersionChooserTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testWindowsIgnoresSeparators()
   {
      assertTrue(RVersionChooser.sameExecutable(
            "C:\\Program Files\\R\\R-4.5.0\\bin\\x64\\Rterm.exe",
            "C:/Program Files/R/R-4.5.0/bin/x64/Rterm.exe",
            true));
      assertTrue(RVersionChooser.sameExecutable(
            "C:\\Program Files\\R\\R-4.5.0\\bin\\x64/Rterm.exe",
            "C:\\Program Files\\R\\R-4.5.0\\bin\\x64\\Rterm.exe",
            true));
   }

   public void testWindowsIgnoresCase()
   {
      assertTrue(RVersionChooser.sameExecutable(
            "C:/Program Files/R/R-4.5.0/bin/x64/Rterm.exe",
            "c:/program files/r/r-4.5.0/bin/x64/rterm.exe",
            true));
   }

   public void testWindowsTakesBinRExeAsThe64BitR()
   {
      assertTrue(RVersionChooser.sameExecutable(
            "C:/Program Files/R/R-4.5.0/bin/R.exe",
            "C:\\Program Files\\R\\R-4.5.0\\bin\\x64\\Rterm.exe",
            true));
      assertFalse(RVersionChooser.sameExecutable(
            "C:/Program Files/R/R-4.5.0/bin/R.exe",
            "C:/Program Files/R/R-4.5.0/bin/i386/Rterm.exe",
            true));
   }

   public void testWindowsTellsInstallationsApart()
   {
      assertFalse(RVersionChooser.sameExecutable(
            "C:/Program Files/R/R-4.4.1/bin/x64/Rterm.exe",
            "C:/Program Files/R/R-4.5.0/bin/x64/Rterm.exe",
            true));
   }

   public void testElsewhereComparesPathsAsGiven()
   {
      assertTrue(RVersionChooser.sameExecutable("/usr/bin/R", "/usr/bin/R", false));
      assertFalse(RVersionChooser.sameExecutable("/usr/local/bin/R", "/opt/R/4.4.1/bin/R", false));
      assertFalse(RVersionChooser.sameExecutable("/opt/R/4.4.1/bin/R", "/opt/r/4.4.1/bin/R", false));
   }
}
