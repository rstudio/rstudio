/*
 * RChunkHeaderParserTests.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.assist;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Map;

public class RChunkHeaderParserTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
   
   public void testRMarkdownChunkHeader()
   {
      String header = "```{r label, echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);
      assertTrue(pieces.containsKey("label"));
      assertTrue(pieces.containsKey("echo"));
      assertTrue(pieces.get("label").contentEquals("label"));
      assertTrue(pieces.get("echo").contentEquals("TRUE"));
   }
   
   public void testNoCommaBeforeFirstItem()
   {
      String header = "```{r echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);
      assertTrue(pieces.containsKey("echo"));
      assertTrue(pieces.get("echo").contentEquals("TRUE"));
   }
   
   public void testCommaBeforeFirstItem()
   {
      String header = "```{r, echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);
      assertTrue(pieces.containsKey("echo"));
      assertTrue(pieces.get("echo").contentEquals("TRUE"));
   }
   
   public void testComplicatedExpression()
   {
      String header = "```{r, echo= {1 + 1}, message=FALSE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);
      assertTrue(pieces.containsKey("echo"));
      assertTrue(pieces.get("echo").contentEquals("{1 + 1}"));
      assertTrue(pieces.containsKey("message"));
      assertTrue(pieces.get("message").contentEquals("FALSE"));
   }

}
