/*
 * DefaultChunkOptionsPopupPanelTests.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionValue;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionValue.OptionLocation;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.DefaultChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.DefaultChunkOptionsPopupPanel.ChunkHeaderInfo;

import java.util.HashMap;

// ------------------------------------------------------
// Suggest keeping these tests in sync with those in 
// RChunkHeaderParserTests.java, currently a
// separate implemention of chunk header parsing.
// ------------------------------------------------------

public class DefaultChunkOptionsPopupPanelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testRMarkdownChunkHeader()
   {
      String header = "```{r label, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("label", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testCommaBeforeChunkLabel()
   {
      String header = "```{r, label, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("label", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testLabelWithDashes()
   {
      String header = "```{r, label-is-super, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("label-is-super", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testSingleQuotedLabel()
   {
      String header = "```{r, 'label-is-super', echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("\'label-is-super\'", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testDoubleQuotedLabel()
   {
      String header = "```{r, \"label-is-super\", echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("\"label-is-super\"", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testNoCommaBeforeFirstItem()
   {
      String header = "```{r echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertTrue(pieces.containsKey("echo"));
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testCommaBeforeFirstItem()
   {
      String header = "```{r, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertTrue(pieces.containsKey("echo"));
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
   }

   public void testComplicatedExpression()
   {
      String header = "```{r, echo= {1 + 1}, message=FALSE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("{1 + 1}", pieces.get("echo").getOptionValue());
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message").getOptionValue());
   }

   public void testSimpleQuotedValue()
   {
      String header = "```{r, fig.cap='hello', message=FALSE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertTrue("contains \"fig.cap\"", pieces.containsKey("fig.cap"));
      assertEquals("\'hello\'", pieces.get("fig.cap").getOptionValue());
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message").getOptionValue());
   }

   public void testQuotedEqualsSign()
   {
      String header = "```{python roger, fig.cap='hello=world', message=FALSE, echo=TRUE, foo='bar=wow'}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("python", extraInfo.chunkPreamble);
      assertEquals("roger", extraInfo.chunkLabel);
      assertTrue("contains \"fig.cap\"", pieces.containsKey("fig.cap"));
      assertEquals("\'hello=world\'", pieces.get("fig.cap").getOptionValue());
      assertTrue("contain message", pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message").getOptionValue());
      assertTrue("contain echo", pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
      assertTrue("contains \"foo\"", pieces.containsKey("foo"));
      assertEquals("\'bar=wow\'", pieces.get("foo").getOptionValue());
      assertEquals(4, pieces.size());
   }

   public void testTrailingComma()
   {
      String header = "```{r fred, echo=TRUE, message=FALSE,}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertEquals("fred", extraInfo.chunkLabel);
      assertTrue("contains echo", pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message").getOptionValue());
      assertEquals(2, pieces.size());
   }

   public void testTrailingCommaAndSpace()
   {
      String header = "```{r fred, echo=TRUE, message=FALSE, }";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertEquals("fred", extraInfo.chunkLabel);
      assertTrue("contains echo", pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message").getOptionValue());
      assertEquals(2, pieces.size());
   }

   public void testNoSpacesAfterCommas()
   {
      String header = "```{r,zoom,echo=TRUE,message=FALSE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertEquals("zoom", extraInfo.chunkLabel);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message").getOptionValue());
   }

   public void testSpacesAroundEqualsSign()
   {
      String header = "```{r, spaces, echo = {1 + 1}, message = FALSE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertEquals("spaces", extraInfo.chunkLabel);
      assertTrue("contains key \"echo\"", pieces.containsKey("echo"));
      assertEquals("{1 + 1}", pieces.get("echo").getOptionValue());
      assertTrue("contains key \"message\"?", pieces.containsKey("message"));
      assertEquals("check message value", "FALSE", pieces.get("message").getOptionValue());
   }

   public void testMultipleFirstLineLabels()
   {
      String header = "```{r my-label, label=my-other-label, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, ChunkOptionValue> pieces = new HashMap<String, ChunkOptionValue>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("my-label", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo").getOptionValue());
      assertEquals(OptionLocation.FirstLine, pieces.get("echo").getLocation());
      assertTrue(pieces.containsKey("label"));
      assertEquals("my-other-label", pieces.get("label").getOptionValue());
      assertEquals(OptionLocation.FirstLine, pieces.get("label").getLocation());
      assertTrue(pieces.containsKey("label"));
   }
}
