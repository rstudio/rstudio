/*
 * ChunkContextUiTests.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import com.google.gwt.junit.client.GWTTestCase;
import org.rstudio.core.client.Pair;

import java.util.List;
import java.util.ArrayList;

public class ChunkContextUiTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
   
   public void testExtractChunkLabel()
   {
      List<Pair<String, String>> testList = new ArrayList<>();
      testList.add(new Pair<>("```{r echo=FALSE}",        ""));
      testList.add(new Pair<>("```{r testingChunks}",        "testingChunks"));
      testList.add(new Pair<>("```{r testingChunks, echo=FALSE}",        "testingChunks"));

      for (Pair<String, String> td  : testList)
      {
         String result = ChunkContextUi.extractChunkLabel(td.first);
         assertTrue(result.equals(td.second));
      }
   }
}
