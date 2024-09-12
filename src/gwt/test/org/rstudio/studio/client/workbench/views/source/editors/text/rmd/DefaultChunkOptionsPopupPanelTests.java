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
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.DefaultChunkOptionsPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.DefaultChunkOptionsPopupPanel.ChunkHeaderInfo;

import java.util.HashMap;

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
      HashMap<String, String> pieces = new HashMap<String, String>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("label", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testCommaBeforeChunkLabel()
   {
      String header = "```{r, label, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, String> pieces = new HashMap<String, String>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("label", extraInfo.chunkLabel);
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(pieces.containsKey("echo"));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testNoCommaBeforeFirstItem()
   {
      String header = "```{r echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, String> pieces = new HashMap<String, String>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertTrue(pieces.containsKey("echo"));
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testCommaBeforeFirstItem()
   {
      String header = "```{r, echo=TRUE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, String> pieces = new HashMap<String, String>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertTrue(pieces.containsKey("echo"));
      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertEquals("TRUE", pieces.get("echo"));
   }

   public void testComplicatedExpression()
   {
      String header = "```{r, echo= {1 + 1}, message=FALSE}";
      ChunkHeaderInfo extraInfo = new ChunkHeaderInfo();
      HashMap<String, String> pieces = new HashMap<String, String>();
      DefaultChunkOptionsPopupPanel.parseChunkHeader(header, "mode/rmarkdown", pieces, extraInfo);

      assertEquals("r", extraInfo.chunkPreamble);
      assertTrue(StringUtil.isNullOrEmpty(extraInfo.chunkLabel));
      assertTrue(pieces.containsKey("echo"));
      assertEquals("{1 + 1}", pieces.get("echo"));
      assertTrue(pieces.containsKey("message"));
      assertEquals("FALSE", pieces.get("message"));
   }
}
