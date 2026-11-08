/*
 * ImageButtonColumnTests.java
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
package org.rstudio.core.client.cellview;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ImageButtonColumnTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static String render(ImageButtonColumn<String> column, String value)
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      column.getCell().render(new Cell.Context(0, 0, value), value, sb);
      return sb.toSafeHtml().asString();
   }

   // One ImageResource2x serves every row of a column, so each render must
   // carry its own row's alt text rather than the first row's.
   public void testEachRowGetsItsOwnAltText()
   {
      ImageButtonColumn<String> column = new ImageButtonColumn<>(
            new ImageResource2x(ThemeResources.INSTANCE.helpSmall2x()),
            value -> {},
            value -> "Help for " + value);

      String first = render(column, "dplyr");
      String second = render(column, "ggplot2");

      assertTrue(first, first.contains("alt=\"Help for dplyr\""));
      assertTrue(first, first.contains("title=\"Help for dplyr\""));
      assertTrue(second, second.contains("alt=\"Help for ggplot2\""));
      assertFalse(second, second.contains("dplyr"));
   }

   public void testTooltipCanDifferFromAltText()
   {
      ImageButtonColumn<String> column = new ImageButtonColumn<>(
            new ImageResource2x(ThemeResources.INSTANCE.helpSmall2x()),
            value -> {},
            value -> "Open " + value + " website",
            value -> "https://" + value + ".org");

      String html = render(column, "dplyr");

      assertTrue(html, html.contains("alt=\"Open dplyr website\""));
      assertTrue(html, html.contains("title=\"https://dplyr.org\""));
      assertFalse(html, html.contains("alt=\"https://"));
   }
}
