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
import java.util.Arrays;
import java.util.List;

import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar.StatusBarIconType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
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
      // GWT.create() only works once the test is running in the GWT environment.
      ViewsSourceConstants constants = GWT.create(ViewsSourceConstants.class);
      StatusBarWidget statusBar = new StatusBarWidget();

      statusBar.showStatus(StatusBarIconType.TYPE_ERROR, "Copilot: Request failed");
      assertVisibleAltTexts(statusBar, constants.errorCapitalized());

      statusBar.showStatus(StatusBarIconType.TYPE_WARNING, "Copilot: Warning text");
      assertVisibleAltTexts(statusBar, constants.warningCapitalized());

      statusBar.showStatus(StatusBarIconType.TYPE_INFO, "Copilot: No completions available.");
      assertVisibleAltTexts(statusBar, constants.informationCapitalized());

      // TYPE_OK shows the same icon as TYPE_INFO, so it gets the same text.
      statusBar.showStatus(StatusBarIconType.TYPE_ERROR, "Copilot: Request failed");
      statusBar.showStatus(StatusBarIconType.TYPE_OK, "Copilot: Completion response received.");
      assertVisibleAltTexts(statusBar, constants.informationCapitalized());

      statusBar.showMessage("Plain message");
      assertVisibleAltTexts(statusBar);
   }

   public void testLoadingSpinnerIsDecorative()
   {
      ViewsSourceConstants constants = GWT.create(ViewsSourceConstants.class);
      StatusBarWidget statusBar = new StatusBarWidget();

      statusBar.showStatus(StatusBarIconType.TYPE_LOADING, "Rendering notebook...");
      List<Element> images = visibleMessageImages(statusBar);
      assertEquals(1, images.size());
      assertTrue(images.get(0).hasAttribute("alt"));
      assertEquals("", images.get(0).getAttribute("alt"));

      // The spinner is hidden, not removed, when an icon replaces it.
      statusBar.showStatus(StatusBarIconType.TYPE_ERROR, "Error creating notebook");
      assertVisibleAltTexts(statusBar, constants.errorCapitalized());
   }

   private static void assertVisibleAltTexts(StatusBarWidget statusBar, String... expected)
   {
      List<String> altTexts = new ArrayList<>();
      for (Element image : visibleMessageImages(statusBar))
         altTexts.add(image.getAttribute("alt"));
      assertEquals(Arrays.asList(expected), altTexts);
   }

   private static List<Element> visibleMessageImages(StatusBarWidget statusBar)
   {
      Element message = statusBar.message_.getElement();
      List<Element> result = new ArrayList<>();
      NodeList<Element> images = message.getElementsByTagName("img");
      for (int i = 0; i < images.getLength(); i++)
      {
         if (isShownWithin(images.getItem(i), message))
            result.add(images.getItem(i));
      }
      return result;
   }

   private static boolean isShownWithin(Element element, Element container)
   {
      for (Element el = element; el != container; el = el.getParentElement())
      {
         if (Display.NONE.getCssName().equals(el.getStyle().getDisplay()))
            return false;
      }
      return true;
   }
}
