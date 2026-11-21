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
      assertTrue(CodeSearchOracle.matchesQuery(xref("exm", "fizz"), "exm:fizz"));
      assertFalse(CodeSearchOracle.matchesQuery(xref("exm", "buzz"), "exm:fizz"));
   }

   public void testFileIgnoresLineAndColumn()
   {
      assertTrue(CodeSearchOracle.matchesQuery(file("addOne.R"), "addOne.R:10"));
      assertTrue(CodeSearchOracle.matchesQuery(file("addOne.R"), "addOne.R:10:4"));
      assertFalse(CodeSearchOracle.matchesQuery(file("addOne.R"), "subOne.R:10"));
   }

   public void testFunctionMatchesWholeQuery()
   {
      assertTrue(CodeSearchOracle.matchesQuery(function("addOne"), "addOne"));
      assertFalse(CodeSearchOracle.matchesQuery(function("addOne"), "addOne:10"));
   }

   public void testWildcardMatchesFromStart()
   {
      assertTrue(CodeSearchOracle.matchesQuery(function("addOne"), "add*ne"));
      assertFalse(CodeSearchOracle.matchesQuery(function("addOne"), "dd*ne"));
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

   private static CodeSearchSuggestion xref(String xrefType, String id)
   {
      SourceItem item = JsonUtils.safeEval(
            "{\"type\": 10, \"name\": \"" + xrefType + ":" + id + "\", " +
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
