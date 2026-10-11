/*
 * ShellWidgetTests.java
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
package org.rstudio.studio.client.common.shell;

import com.google.gwt.junit.client.GWTTestCase;

public class ShellWidgetTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testErrorKeyStripsErrorPrefix()
   {
      assertEquals("object 'x' not found",
                   ShellWidget.errorKey("Error in foo(x) : object 'x' not found\n"));
      assertEquals("boom", ShellWidget.errorKey("Error: boom"));
   }

   public void testErrorKeyStripsOnlyUpToFirstColon()
   {
      assertEquals("a: b", ShellWidget.errorKey("Error in f() : a: b"));
   }

   public void testErrorKeyStripsAnsiCodes()
   {
      assertEquals("boom", ShellWidget.errorKey("\033[1mError\033[0m: \033[31mboom\033[0m"));
   }

   public void testErrorKeyLeavesColonFreeTextAlone()
   {
      assertEquals("Transferred 12 of 5000 objects...",
                   ShellWidget.errorKey("\rTransferred 12 of 5000 objects...\n"));
   }

   // The prefix regex used to be unanchored, which is quadratic on text with
   // no colon: a few hundred KB of progress output (one stderr chunk from
   // gert::git_clone(verbose = TRUE)) took many seconds. With the anchored
   // pattern this is instant; unanchored, it blows the test timeout.
   public void testErrorKeyIsFastOnLargeColonFreeOutput()
   {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 12000; i++)
         sb.append("\rTransferred ").append(i).append(" of 12000 objects...");

      String key = ShellWidget.errorKey(sb.toString());
      assertTrue(key.endsWith("Transferred 11999 of 12000 objects..."));
   }
}
