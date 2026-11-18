/*
 * ConsoleLanguageTrackerTests.java
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
package org.rstudio.studio.client.workbench.views.console.shell;

import com.google.gwt.junit.client.GWTTestCase;

// These mirror SessionConsoleInputTests.cpp: the client and the session must
// agree on where console input takes the console.
public class ConsoleLanguageTrackerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static final String R = ConsoleLanguageTracker.LANGUAGE_R;
   private static final String PYTHON = ConsoleLanguageTracker.LANGUAGE_PYTHON;
   private static final String JULIA = ConsoleLanguageTracker.LANGUAGE_JULIA;

   public void testReplEntryAndExitAreFollowed()
   {
      assertEquals(PYTHON, ConsoleLanguageTracker.languageAfterInput(R, "reticulate::repl_python()"));
      assertEquals(PYTHON, ConsoleLanguageTracker.languageAfterInput(R, "repl_python()"));
      assertEquals(JULIA, ConsoleLanguageTracker.languageAfterInput(R, ".rs.julia.repl()"));

      for (String exit : new String[] { "quit", "exit", "quit()", "exit()" })
      {
         assertEquals(R, ConsoleLanguageTracker.languageAfterInput(PYTHON, exit));
         assertEquals(R, ConsoleLanguageTracker.languageAfterInput(JULIA, exit));
         assertEquals(R, ConsoleLanguageTracker.languageAfterInput(R, exit));
      }

      assertEquals(R, ConsoleLanguageTracker.languageAfterInput(R, "1 + 1"));
      assertEquals(JULIA, ConsoleLanguageTracker.languageAfterInput(JULIA, "1 + 1"));
   }

   public void testReplEntryIsOnlyRecognizedInR()
   {
      assertEquals(PYTHON, ConsoleLanguageTracker.languageAfterInput(PYTHON, ".rs.julia.repl()"));
      assertEquals(JULIA, ConsoleLanguageTracker.languageAfterInput(JULIA, "reticulate::repl_python()"));
   }

   public void testInputIsFollowedLineByLine()
   {
      assertEquals(JULIA, ConsoleLanguageTracker.languageAfterInput(PYTHON, "quit\n.rs.julia.repl()"));
      assertEquals(R, ConsoleLanguageTracker.languageAfterInput(R, ".rs.julia.repl()\nx = 1\nexit()"));
      assertEquals(PYTHON, ConsoleLanguageTracker.languageAfterInput(JULIA, "exit\nrepl_python()\nx = 1"));
   }

   public void testSwitchInputGoesByWayOfR()
   {
      assertEquals("", ConsoleLanguageTracker.consoleLanguageSwitch(R, R));
      assertEquals("", ConsoleLanguageTracker.consoleLanguageSwitch(JULIA, JULIA));
      assertEquals("reticulate::repl_python()", ConsoleLanguageTracker.consoleLanguageSwitch(R, PYTHON));
      assertEquals(".rs.julia.repl()", ConsoleLanguageTracker.consoleLanguageSwitch(R, JULIA));
      assertEquals("quit", ConsoleLanguageTracker.consoleLanguageSwitch(PYTHON, R));
      assertEquals("quit", ConsoleLanguageTracker.consoleLanguageSwitch(JULIA, R));
      assertEquals("quit\n.rs.julia.repl()", ConsoleLanguageTracker.consoleLanguageSwitch(PYTHON, JULIA));
      assertEquals("quit\nreticulate::repl_python()", ConsoleLanguageTracker.consoleLanguageSwitch(JULIA, PYTHON));

      // no language the console has
      assertEquals("", ConsoleLanguageTracker.consoleLanguageSwitch(R, "Stan"));
      assertEquals("", ConsoleLanguageTracker.consoleLanguageSwitch(JULIA, "Stan"));
   }

   public void testSwitchInputReachesItsTarget()
   {
      String[] languages = { R, PYTHON, JULIA };
      for (String from : languages)
      {
         for (String to : languages)
         {
            String input = ConsoleLanguageTracker.consoleLanguageSwitch(from, to);
            assertEquals(from + " -> " + to, to, ConsoleLanguageTracker.languageAfterInput(from, input));
         }
      }
   }
}
