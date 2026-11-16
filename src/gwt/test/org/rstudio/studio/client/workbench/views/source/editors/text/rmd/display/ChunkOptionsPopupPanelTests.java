/*
 * ChunkOptionsPopupPanelTests.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display;

import static org.rstudio.core.client.dom.LabelTestHelper.labelTextFor;

import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display.ChunkOptionValue.OptionLocation;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;

public class ChunkOptionsPopupPanelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testEnginePathBoxIsLabelled()
   {
      ChunkOptionsPopupPanel panel = newCustomEnginePanel();
      assertEquals("Engine path:", labelTextFor(panel.getElement(), panel.enginePathBox_.getElement()));
   }

   public void testEngineOptionsBoxIsLabelled()
   {
      ChunkOptionsPopupPanel panel = newCustomEnginePanel();
      assertEquals("Engine options:", labelTextFor(panel.getElement(), panel.engineOptsBox_.getElement()));
   }

   public void testEnginePathBrowseButtonIsNamed()
   {
      ChunkOptionsPopupPanel panel = newCustomEnginePanel();
      NodeList<Element> buttons = panel.getElement().getElementsByTagName("button");
      Element browse = null;
      for (int i = 0; i < buttons.getLength(); i++)
      {
         if (buttons.getItem(i).getInnerText().trim().equals("..."))
            browse = buttons.getItem(i);
      }

      assertNotNull("the engine path browse button must exist", browse);
      assertEquals("Select Engine", browse.getAttribute("aria-label"));
      assertEquals("Select Engine", browse.getTitle());
   }

   private static ChunkOptionsPopupPanel newCustomEnginePanel()
   {
      return new CustomEngineChunkOptionsPopupPanel(
            "python", OptionLocation.FirstLine, false);
   }
}
