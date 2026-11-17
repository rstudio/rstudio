/*
 * AppCommandMenuImageTests.java
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
package org.rstudio.core.client.command;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ImageResource;

public class AppCommandMenuImageTests extends GWTTestCase
{
   private static final String GLYPH_CLASSES =
         "rstudio-forced-colors-glyph rstudio-glyph-menu-check";

   private static String menuLabel(ImageResource icon)
   {
      return AppCommand.formatMenuLabel(new ImageResource2x(icon), "Item", null);
   }

   public void testMenuCheckIsTaggedForContrastThemes()
   {
      assertTrue(menuLabel(ThemeResources.INSTANCE.menuCheck2x()).contains(GLYPH_CLASSES));
   }

   public void testInvertedMenuCheckIsTaggedForContrastThemes()
   {
      assertTrue(menuLabel(ThemeResources.INSTANCE.menuCheckInverted2x()).contains(GLYPH_CLASSES));
   }

   public void testOtherMenuIconsAreNotTagged()
   {
      String html = menuLabel(ThemeResources.INSTANCE.executeChunk2x());
      assertTrue(html.contains("<img"));
      assertFalse(html.contains("rstudio-forced-colors-glyph"));
      assertFalse(html.contains("class=\"\""));
   }

   public void testIconCellIsHiddenFromAssistiveTechnology()
   {
      // Hidden with or without an icon: with none, its <br/> placeholder would
      // otherwise add leading whitespace to the item's accessible name.
      ImageResource[] icons = { null, ThemeResources.INSTANCE.executeChunk2x() };
      for (ImageResource icon : icons)
      {
         Element label = Document.get().createDivElement();
         label.setInnerHTML(icon == null
               ? AppCommand.formatMenuLabel(null, "Item", "Ctrl+K")
               : menuLabel(icon));
         Element iconCell = label.getElementsByTagName("td").getItem(0);
         assertEquals("true", iconCell.getAttribute("aria-hidden"));
      }
   }

   public void testLayoutElementsArePresentational()
   {
      // Chrome exposes table parts without a role as nameless generic nodes,
      // which VoiceOver announces as "group" after the menu item's text.
      ImageResource2x rightImage =
            new ImageResource2x(ThemeResources.INSTANCE.executeChunk2x());
      String[] labels = {
            AppCommand.formatMenuLabel(null, "Item", "Ctrl+K"),
            AppCommand.formatMenuLabel(null, "Item", null),
            AppCommand.formatMenuLabel(null, "Item", false, null, rightImage, "Run"),
      };
      for (String html : labels)
      {
         Element label = Document.get().createDivElement();
         label.setInnerHTML(html);
         assertEquals(1, label.getElementsByTagName("tbody").getLength());
         for (String tag : new String[] { "table", "tbody", "tr", "td" })
         {
            NodeList<Element> elements = label.getElementsByTagName(tag);
            assertTrue(tag, elements.getLength() > 0);
            for (int i = 0; i < elements.getLength(); i++)
               assertEquals(tag, "presentation", elements.getItem(i).getAttribute("role"));
         }
      }
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
}
