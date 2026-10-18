/*
 * StatusBarWidgetTests.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar.StatusBarIconType;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;

public class StatusBarWidgetTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testStatusIconAltTextNamesItsType()
   {
      StatusBarWidget statusBar = new StatusBarWidget();

      statusBar.showStatus(StatusBarIconType.TYPE_ERROR, "Copilot: Request failed");
      assertTrue(altTexts(statusBar).contains("Error"));

      statusBar.showStatus(StatusBarIconType.TYPE_WARNING, "Copilot: Warning text");
      assertTrue(altTexts(statusBar).contains("Warning"));
      assertFalse(altTexts(statusBar).contains("Error"));

      statusBar.showStatus(StatusBarIconType.TYPE_INFO, "Copilot: No completions available.");
      assertTrue(altTexts(statusBar).contains("Information"));
      assertFalse(altTexts(statusBar).contains("Warning"));

      // TYPE_OK shows the same icon as TYPE_INFO, so it gets the same text.
      statusBar.showStatus(StatusBarIconType.TYPE_ERROR, "Copilot: Request failed");
      statusBar.showStatus(StatusBarIconType.TYPE_OK, "Copilot: Completion response received.");
      assertTrue(altTexts(statusBar).contains("Information"));
      assertFalse(altTexts(statusBar).contains("Error"));
   }

   public void testLoadingSpinnerIsDecorative()
   {
      StatusBarWidget statusBar = new StatusBarWidget();
      statusBar.showStatus(StatusBarIconType.TYPE_LOADING, "Rendering notebook...");

      Element spinner = null;
      NodeList<Element> images = statusBar.getElement().getElementsByTagName("img");
      for (int i = 0; i < images.getLength(); i++)
      {
         if (images.getItem(i).getAttribute("src").startsWith("data:image/svg+xml"))
            spinner = images.getItem(i);
      }

      assertNotNull(spinner);
      assertTrue(spinner.hasAttribute("alt"));
      assertEquals("", spinner.getAttribute("alt"));
   }

   private static List<String> altTexts(StatusBarWidget statusBar)
   {
      List<String> result = new ArrayList<>();
      NodeList<Element> images = statusBar.getElement().getElementsByTagName("img");
      for (int i = 0; i < images.getLength(); i++)
         result.add(images.getItem(i).getAttribute("alt"));
      return result;
   }
}
