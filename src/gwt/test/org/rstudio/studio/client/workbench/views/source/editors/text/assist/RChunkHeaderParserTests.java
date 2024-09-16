/*
 * RChunkHeaderParserTests.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.assist;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Map;

// ------------------------------------------------------
// Suggest keeping these tests in sync with those in 
// DefaultChunkOptionsPopupPanelTests.java, currently a
// separate implemention of chunk header parsing.
// ------------------------------------------------------

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
      assertTrue(pieces.containsKey("engine"));
      assertTrue(pieces.containsKey("label"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("\"r\"", pieces.get("engine"));
      assertEquals("\"label\"", pieces.get("label"));
      assertTrue(pieces.get("echo").contentEquals("TRUE"));
   }

   public void testCommaBeforeChunkLabel()
   {
      String header = "```{r, label, echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);
      assertTrue(pieces.containsKey("engine"));
      assertTrue(pieces.containsKey("label"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("\"r\"", pieces.get("engine"));
      assertEquals("\"label\"", pieces.get("label"));
      assertTrue(pieces.get("echo").contentEquals("TRUE"));
   }

   public void testLabelWithDashes()
   {
      String header = "```{r, label-is-super, echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\"label-is-super\"", pieces.get("label"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testSingleQuotedLabel()
   {
      String header = "```{r, 'label-is-super', echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\'label-is-super\'", pieces.get("label"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testDoubleQuotedLabel()
   {
      String header = "```{r, \"label-is-super\", echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\"label-is-super\"", pieces.get("label"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
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

   public void testSimpleQuotedValue()
   {
      String header = "```{r, fig.cap='hello', message=FALSE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue("contains \"fig.cap\"", pieces.containsKey("fig.cap"));
      assertEquals("\'hello\'", pieces.get("fig.cap"));
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message"));
   }

   public void testQuotedEqualsSign()
   {
      String header = "```{r, roger, fig.cap='hello=world', message=FALSE, echo=TRUE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertEquals(5, pieces.size());
      assertTrue(pieces.containsKey("engine"));
      assertEquals(header, "\"r\"", pieces.get("engine"));
      assertTrue("contains \"fig.cap\"", pieces.containsKey("fig.cap"));
      assertEquals("\'hello=world\'", pieces.get("fig.cap"));
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testTrailingComma()
   {
      String header = "```{r fred, echo=TRUE, message=FALSE,}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("engine"));
      assertEquals(header, "\"r\"", pieces.get("engine"));
      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\"fred\"", pieces.get("label"));
      assertTrue("contains echo", pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message"));
      assertEquals(4, pieces.size());
   }

   public void testTrailingCommaAndSpace()
   {
      String header = "```{r fred, echo=TRUE, message=FALSE, }";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("engine"));
      assertEquals(header, "\"r\"", pieces.get("engine"));
      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\"fred\"", pieces.get("label"));
      assertTrue("contains echo", pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message"));
      assertEquals(4, pieces.size());
   }

   public void testNoSpacesAfterCommas()
   {
      String header = "```{r,zoom,echo=TRUE,message=FALSE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("engine"));
      assertEquals(header, "\"r\"", pieces.get("engine"));
      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\"zoom\"", pieces.get("label"));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message"));
   }

   public void testSpacesAroundEqualsSign()
   {
      String header = "```{r, spaces, echo = {1 + 1}, message = FALSE}";
      Map<String, String> pieces = RChunkHeaderParser.parse(header);

      assertTrue(pieces.containsKey("engine"));
      assertEquals(header, "\"r\"", pieces.get("engine"));
      assertTrue(pieces.containsKey("label"));
      assertEquals(header, "\"spaces\"", pieces.get("label"));
       assertTrue("contains key \"echo\"", pieces.containsKey("echo"));
      assertEquals("{1 + 1}", pieces.get("echo"));
      assertTrue("contains key \"message\"?", pieces.containsKey("message"));
      assertEquals("check message value", "FALSE", pieces.get("message"));
   }
}
