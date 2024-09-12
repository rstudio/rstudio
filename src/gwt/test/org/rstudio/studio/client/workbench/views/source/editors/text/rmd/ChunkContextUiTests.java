/*
 * ChunkContextUiTests.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import com.google.gwt.junit.client.GWTTestCase;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi.ChunkLabelInfo;

import java.util.List;
import java.util.ArrayList;

public class ChunkContextUiTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
   private static class TestCase
   {
      public TestCase(String input, String label, int nextTokenIdx)
      {
         this.input = input;
         this.label = label;
         this.nextTokenIdx = nextTokenIdx;
      }
   
      public String input;
      public String label;
      public int nextTokenIdx;
   }

   public void testExtractChunkLabel()
   {
      // input string, expected results, expected position
      List<TestCase> tests = new ArrayList<>();
      tests.add(new TestCase("```{r}",                            "",                6));
      tests.add(new TestCase("```{r,foo=BAR}",                    "",                5));
      tests.add(new TestCase("```{r, foo=BAR}",                   "",                6));
      tests.add(new TestCase("```{r testingChunks}",              "testingChunks",  20));
      tests.add(new TestCase("```{r  testingChunks  }",           "testingChunks",  23));
      tests.add(new TestCase("```{r   testing-Chunks}",           "testing-Chunks", 23));
      tests.add(new TestCase("```{r, testingChunks}",             "testingChunks",  21));
      tests.add(new TestCase("```{r,testingChunks}",              "testingChunks",  20));
      tests.add(new TestCase("```{r echo=FALSE}",                 "",                5));
      tests.add(new TestCase("```{r,echo=FALSE}",                 "",                5));
      tests.add(new TestCase("```{r, echo=FALSE}",                "",                6));
      tests.add(new TestCase("```{r testingChunks, echo=FALSE}",  "testingChunks",  19));
      tests.add(new TestCase("```{r testingChunks,echo=FALSE}",   "testingChunks",  19));
      tests.add(new TestCase("```{r, testingChunks, echo=FALSE}", "testingChunks",  20));
      tests.add(new TestCase("```{r, label, echo=FALSE, ab=cd}",  "label",          12));
      tests.add(new TestCase("```{r,label,echo=FALSE,ab=cd}",     "label",          11));
      tests.add(new TestCase("```{python}",                       "",               11));
      tests.add(new TestCase("```{python,foo=BAR}",               "",               10));
      tests.add(new TestCase("```{python, foo=BAR}",              "",               11));
      tests.add(new TestCase("```{bash testingChunks}",           "testingChunks",  23));
      tests.add(new TestCase("```{Rcpp  testingChunks  }",        "testingChunks",  26));
      tests.add(new TestCase("```{d3   testing-Chunks}",          "testing-Chunks", 24));
      tests.add(new TestCase("```{stan, testingChunks}",          "testingChunks",  24));
      tests.add(new TestCase("```{sql,testingChunks}",            "testingChunks",  22));
      tests.add(new TestCase("```{python echo=FALSE}",            "",               10));
      tests.add(new TestCase("```{python,echo=FALSE}",            "",               10));
      tests.add(new TestCase("```{python, echo=FALSE}",           "",               11));
      tests.add(new TestCase("```{python testingChunks, a=b}",    "testingChunks",  24));
      tests.add(new TestCase("```{d3 testingChunks,echo=FALSE}",  "testingChunks",  20));
      tests.add(new TestCase("```{Rcpp, testingChunks, ab=cd}",   "testingChunks",  23));
      tests.add(new TestCase("```{d3, label, echo=FALSE, ab=cd}", "label",          13));
      tests.add(new TestCase("```{stan,label,echo=FALSE,ab=cd}",  "label",          14));

      for (TestCase test: tests)
      {
         ChunkLabelInfo result = ChunkContextUi.extractChunkLabel(test.input);
         assertEquals(test.input, test.label, result.label);
         assertEquals(test.input, test.nextTokenIdx, result.nextSepIndex);
      }
   }
}