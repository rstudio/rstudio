/*
 * CodeSearchOracleTests.java
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
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.studio.client.workbench.codesearch.model.FileItem;
import org.rstudio.studio.client.workbench.codesearch.model.SourceItem;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.junit.client.GWTTestCase;

public class CodeSearchOracleTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Narrowing the results for "exm" as the user goes on to type ":fizz".
   public void testXRefMatchesTextAfterColon()
   {
      assertTrue(matches(xref("exm", "fizz"), "exm:fizz"));
      assertFalse(matches(xref("exm", "buzz"), "exm:fizz"));
   }

   public void testFileIgnoresLineAndColumn()
   {
      assertTrue(matches(file("addOne.R"), "addOne.R:10"));
      assertTrue(matches(file("addOne.R"), "addOne.R:10:4"));
      assertFalse(matches(file("addOne.R"), "subOne.R:10"));
   }

   public void testFunctionMatchesWholeQuery()
   {
      assertTrue(matches(function("addOne"), "addOne"));
      assertFalse(matches(function("addOne"), "addOne:10"));
   }

   public void testWildcardMatchesText()
   {
      assertTrue(matches(function("addOne"), "add*ne"));
      assertFalse(matches(function("addOne"), "add*x"));
   }

   public void testXRefScoresAgainstTextAfterColon()
   {
      assertEquals(0, CodeSearchOracle.scoreMatch(xref("exm", "fizz"), "exm:fizz"));
      assertTrue(CodeSearchOracle.scoreMatch(xref("exm", "fizz"), "exm:fizz") <
                 CodeSearchOracle.scoreMatch(xref("exm", "buzz"), "exm:fizz"));
   }

   public void testFileScoresWithoutLineAndColumn()
   {
      assertEquals(CodeSearchOracle.scoreMatch(file("addOne.R"), "addOne.R"),
                   CodeSearchOracle.scoreMatch(file("addOne.R"), "addOne.R:10"));
   }

   public void testNamedXRefMatchesLabelOrName()
   {
      CodeSearchSuggestion theorem = xref("thm", "pyth", "Pythagoras");
      assertTrue(matches(theorem, "thm:pyth"));
      assertTrue(matches(theorem, "Pythag"));
      assertFalse(matches(theorem, "Euclid"));
   }

   // R functions send their signature as extra info; the query doesn't match it.
   public void testFunctionDoesNotMatchExtraInfo()
   {
      SourceItem item = JsonUtils.safeEval(
            "{\"type\": 1, \"name\": \"addOne\", \"extra_info\": \"(x)\", " +
            "\"context\": \"~/addOne.R\"}");
      assertFalse(matches(new CodeSearchSuggestion(item, null), "x"));
   }

   public void testNamedXRefRanksByName()
   {
      assertEquals(0, CodeSearchOracle.scoreMatch(
            xref("thm", "pyth", "Pythagoras"), "Pythagoras"));
      assertTrue(CodeSearchOracle.scoreMatch(xref("thm", "pyth", "Pythagoras"), "Pythagoras") <
                 CodeSearchOracle.scoreMatch(xref("thm", "pyth"), "Pythagoras"));
   }

   // "Pythagoras" lacks "q", yet would outscore the label's late matches.
   public void testUnmatchedNameDoesNotAffectRank()
   {
      assertEquals(CodeSearchOracle.scoreMatch(xref("thm", "very-long-label-pq"), "pq"),
                   CodeSearchOracle.scoreMatch(
                         xref("thm", "very-long-label-pq", "Pythagoras"), "pq"));
   }

   // The server reads '*' literally in cross-references, so a fresh search
   // for "Pyth*" doesn't list the theorem; narrowing to it mustn't either.
   public void testWildcardDoesNotMatchName()
   {
      CodeSearchSuggestion theorem = xref("thm", "pyth", "Pythagoras");
      assertFalse(matches(theorem, "Pyth*"));
      assertEquals(CodeSearchOracle.scoreMatch(xref("thm", "pyth"), "Pyth*"),
                   CodeSearchOracle.scoreMatch(theorem, "Pyth*"));
   }

   private static boolean matches(CodeSearchSuggestion suggestion, String query)
   {
      return CodeSearchOracle.matchesQuery(
            suggestion, query, CodeSearchOracle.wildcardPattern(query));
   }

   private static CodeSearchSuggestion xref(String xrefType, String id)
   {
      return xref(xrefType, id, "");
   }

   private static CodeSearchSuggestion xref(String xrefType, String id, String name)
   {
      SourceItem item = JsonUtils.safeEval(
            "{\"type\": 10, \"name\": \"" + xrefType + ":" + id + "\", " +
            "\"extra_info\": \"" + name + "\", " +
            "\"context\": \"~/book\", \"metadata\": {\"type\": \"xref\", \"xref\": " +
            "{\"file\": \"01-intro.Rmd\", \"type\": \"" + xrefType + "\", " +
            "\"id\": \"" + id + "\"}}}");
      return new CodeSearchSuggestion(item, null);
   }

   private static CodeSearchSuggestion function(String name)
   {
      SourceItem item = JsonUtils.safeEval(
            "{\"type\": 1, \"name\": \"" + name + "\", \"context\": \"~/" + name + ".R\"}");
      return new CodeSearchSuggestion(item, null);
   }

   private static CodeSearchSuggestion file(String filename)
   {
      FileItem item = JsonUtils.safeEval(
            "{\"filename\": \"" + filename + "\", \"path\": \"~/" + filename + "\"}");
      return new CodeSearchSuggestion(item);
   }
}
